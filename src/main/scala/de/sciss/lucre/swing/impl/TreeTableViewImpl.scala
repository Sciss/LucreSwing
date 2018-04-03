/*
 *  TreeTableViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing
package impl

import java.awt
import java.awt.EventQueue
import javax.swing.event.CellEditorListener
import javax.swing.table.{DefaultTableCellRenderer, TableCellEditor}
import javax.swing.{CellEditor, DropMode, JTable}

import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm.{Disposable, Identifiable, IdentifierMap}
import de.sciss.lucre.swing.TreeTableView.{ModelUpdate, Handler}
import de.sciss.lucre.stm
import de.sciss.model.impl.ModelImpl
import de.sciss.serial.Serializer
import de.sciss.treetable.TreeTable.Path
import de.sciss.treetable.j.TreeTableCellEditor
import de.sciss.treetable.{AbstractTreeModel, TreeColumnModel, TreeTable, TreeTableCellRenderer, TreeTableSelectionChanged, j}

import scala.annotation.tailrec
import scala.collection.breakOut
import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.stm.TxnLocal
import scala.swing.{Component, ScrollPane}

object TreeTableViewImpl {
  var DEBUG = false

  private object NodeViewImpl {
    sealed trait Base[S <: Sys[S], Node, Branch, Data] extends Disposable[S#Tx] {
      def isLeaf: Boolean
      def numChildren: Int
      def parentOption1: Option[BranchOrRoot[S, Node, Branch, Data]]

      protected def observer: Disposable[S#Tx]

      def dispose()(implicit tx: S#Tx): Unit = observer.dispose()
    }

    sealed trait BranchOrRoot[S <: Sys[S], Node, Branch, Data] extends Base[S, Node, Branch, Data] {
      // ---- abstract ----

      def branchH: stm.Source[S#Tx, Branch]

      // ---- impl ----

      final var children    : Vec[NodeViewImpl[S, Node, Branch, Data]] = Vector.empty
      final def isLeaf      : Boolean = false
      final def numChildren : Int     = children.size
    }

    class BranchImpl[S <: Sys[S], Node, Branch, Data](val parentImpl: BranchOrRoot[S, Node, Branch, Data],
                                          val branchH: stm.Source[S#Tx, Branch],
                                          val renderData: Data,
                                          val modelData: stm.Source[S#Tx, Node],
                                          protected val observer: Disposable[S#Tx])
      extends BranchOrRoot[S, Node, Branch, Data] with NodeViewImpl[S, Node, Branch, Data] {

      override def toString = s"Branch($modelData, $renderData)"
    }

    class Leaf[S <: Sys[S], Node, Branch, Data](val parentImpl: BranchOrRoot[S, Node, Branch, Data],
                                        val renderData: Data,
                                        val modelData: stm.Source[S#Tx, Node],
                                        protected val observer: Disposable[S#Tx])
      extends NodeViewImpl[S, Node, Branch, Data] {

      def isLeaf = true
      def numChildren = 0

      override def toString = s"Leaf($modelData, $renderData)"
    }

    class Root[S <: Sys[S], Node, Branch, Data](val branchH: stm.Source[S#Tx, Branch],
                                                protected val observer: Disposable[S#Tx])
      extends BranchOrRoot[S, Node, Branch, Data] {

      def parentOption1: Option[NodeViewImpl.BranchOrRoot[S, Node, Branch, Data]] = None

      override def toString = "Root"
    }
  }
  private sealed trait NodeViewImpl[S <: Sys[S], Node, Branch, Data]
    extends NodeViewImpl.Base[S, Node, Branch, Data] with TreeTableView.NodeView[S, Node, Branch, Data] {

    // ---- abstract ----

    def parentImpl: NodeViewImpl.BranchOrRoot[S, Node, Branch, Data]

    val renderData: Data

    def numChildren: Int

    // ---- impl ----

    final def parentOption1: Option[NodeViewImpl.BranchOrRoot[S, Node, Branch, Data]] = Some(parentImpl)

    final def parentView: Option[NodeViewImpl.BranchImpl[S, Node, Branch, Data]] = parentImpl match {
      case b: NodeViewImpl.BranchImpl[S, Node, Branch, Data] => Some(b)
      case _ => None
    }

    final def parent(implicit tx: S#Tx): Branch = parentImpl.branchH()
  }

  def apply[S <: Sys[S], Node <: Identifiable[S#Id], Branch <: Node, Data](
       root: Branch,
       handler: Handler[S, Node, Branch, Data])(implicit tx: S#Tx, nodeSerializer: Serializer[S#Tx, S#Acc, Node],
                                                   branchSerializer: Serializer[S#Tx, S#Acc, Branch])
      : TreeTableView[S, Node, Branch, Data] = {
    val _handler  = handler
    val _root     = root
    new Impl[S, Node, Branch, Data] {
      val mapViews    : IdentifierMap[S#Id, S#Tx, VNode   ] = tx.newInMemoryIdMap   // node Ids to renderers
      val mapBranches : IdentifierMap[S#Id, S#Tx, VBranchL] = tx.newInMemoryIdMap   // node Ids to renderers
      val handler     : Handler[S, Node, Branch, Data]      = _handler
      val rootView    = new NodeViewImpl.Root[S, Node, Branch, Data](tx.newHandle(_root),
        _handler.observe(_root, processUpdateFun))
      mapBranches.put(_root.id, rootView)
      handler.children(_root).toList.zipWithIndex.foreach { case (c, ci) =>
        elemAdded(rootView, ci, c, refresh = false)
      }
//      val observer = _root.changed.react { implicit tx => upd =>
//        processUpdate(upd)
//      }

      deferTx {
        guiInit()
      }
    }
  }

  private abstract class Impl[S <: Sys[S], Node <: Identifiable[S#Id], Branch <: Node, Data](
      implicit nodeSerializer: Serializer[S#Tx, S#Acc, Node], branchSerializer: Serializer[S#Tx, S#Acc, Branch])
    extends ComponentHolder[Component] with TreeTableView[S, Node, Branch, Data] with ModelImpl[TreeTableView.Update] {
    view =>

    type VBranch  = NodeViewImpl.BranchImpl   [S, Node, Branch, Data]
    type VBranchL = NodeViewImpl.BranchOrRoot [S, Node, Branch, Data]
    type VLeaf    = NodeViewImpl.Leaf         [S, Node, Branch, Data]
    type VNode    = NodeViewImpl              [S, Node, Branch, Data]
    type VNodeL   = NodeViewImpl.Base         [S, Node, Branch, Data]
    type VRoot    = NodeViewImpl.Root         [S, Node, Branch, Data]
    type TPath    = TreeTable.Path[VBranch]

    type NodeView = VNode // alias in the interface

    type Update = ModelUpdate[Node, Branch]

    protected def rootView    : VRoot
    protected def mapViews    : IdentifierMap[S#Id, S#Tx, VNode   ]
    protected def mapBranches : IdentifierMap[S#Id, S#Tx, VBranchL]
    // protected def observer    : Disposable[S#Tx]
    protected def handler     : Handler[S, Node, Branch, Data]

    private val didInsert = TxnLocal(false)

    def root: stm.Source[S#Tx, Branch] = rootView.branchH

    def nodeView(node: Node)(implicit tx: S#Tx): Option[NodeView] = mapViews.get(node.id)

    protected final val processUpdateFun: S#Tx => Update => Unit = tx => upd => processUpdate(upd)(tx)

    private object treeModel extends AbstractTreeModel[VNodeL] {
      lazy val root: VNodeL = rootView // ! must be lazy

      def getChildCount(parent: VNodeL): Int = parent.numChildren

      def getChild(parent: VNodeL, index: Int): VNode = parent match {
        case b: VBranchL => b.children(index)
        case _           => sys.error(s"parent $parent is not a branch")
      }

      def isLeaf(node: VNodeL): Boolean = node.isLeaf

      def getIndexOfChild(parent: VNodeL, child: VNodeL): Int = parent match {
        case b: VBranchL => b.children.indexOf(child)
        case _           => sys.error(s"parent $parent is not a branch")
      }

      def getParent(node: VNodeL): Option[VNodeL] = node.parentOption1

      //      def getParent(node: VNodeL): Option[VNodeL] = node match {
      //        case _: VRoot => None
      //        case n: VNode => n.parentOption
      //      }

      def getPath(node: VNode): TreeTable.Path[VNodeL] = {
        @tailrec def loop(n: VNodeL, res: TreeTable.Path[VNodeL]): TreeTable.Path[VNodeL] = {
          val res1 = n +: res
          n.parentOption1 match {
            case Some(p)  => loop(p, res1)
            case _        => res1
          }
        }
        loop(node, Vec.empty)
      }

      def valueForPathChanged(path: TreeTable.Path[VNodeL], newValue: VNodeL): Unit =
        if (DEBUG) println(s"valueForPathChanged($path, $newValue)")

      def elemAdded(parent: VBranchL, idx: Int, view: VNode): Unit = {
        elemAddedNoRefresh(parent, idx, view)
        fireNodesInserted(view)
      }

      def elemAddedNoRefresh(parent: VBranchL, idx: Int, view: VNode): Unit = {
        if (DEBUG) println(s"model.elemAdded($parent, $idx, $view)")
        val g       = parent  // Option.getOrElse(_root)
        require(idx >= 0 && idx <= g.children.size, idx.toString)
        g.children  = g.children.patch(idx, Vector(view), 0)
      }

      def elemRemoved(parent: VBranchL, idx: Int): Unit = {
        if (DEBUG) println(s"model.elemRemoved($parent, $idx)")
        require(idx >= 0 && idx < parent.children.size, idx.toString)
        val v       = parent.children(idx)
        // this is insane. the tree UI still accesses the model based on the previous assumption
        // about the number of children, it seems. therefore, we must not update children before
        // returning from fireNodesRemoved.
        fireNodesRemoved(v)
        parent.children  = parent.children.patch(idx, Vector.empty, 1)
      }

      def elemUpdated(view: VNode): Unit = {
        if (DEBUG) println(s"model.elemUpdated($view)")
        fireNodesChanged(view)
      }
    }

    private var t: TreeTable[VNodeL, TreeColumnModel[VNodeL]] = _

    def treeTable: TreeTable[_, _] = t

    // def selection: List[VNode] = t.selection.paths.flatMap(_.lastOption)(breakOut)

    //    def dropLocation: Option[NodeView] = t.dropLocation.flatMap { dl => dl.path match {
    //      case _ :+ (last: NodeView) => Some(last)
    //      case _ => None
    //    }}

    def dropLocation: Option[TreeTable.DropLocation[NodeView]] =
      Option(t.peer.getDropLocation).map { j =>
        new TreeTable.DropLocation[NodeView](j) {
          override def path: Path[NodeView] = super.path.drop(1)
        }
      }

    def selection: List[VNode] = t.selection.paths.collect {
      case _ /* init */ :+ (last: VNode) => last
    } (breakOut)

    def markInsertion()(implicit tx: S#Tx): Unit = didInsert.update(true)(tx.peer)

    def insertionPoint(implicit tx: S#Tx): (Branch, Int) = {
      if (!EventQueue.isDispatchThread) throw new IllegalStateException("Must be called on the EDT")
      selection match {
        case singleView :: Nil =>
          val single = singleView.modelData()
          handler.branchOption(single).fold[(Branch, Int)] {
            val parentView    = singleView.parentImpl // Option.getOrElse(rootView)
            val parentBranch  = parentView match {
              case b: VBranch => handler.branchOption(b.modelData()).getOrElse(throw new IllegalStateException())
              case r: VRoot   => r.branchH()
            }
            val idx = handler.children(parentBranch).toIndexedSeq.indexOf(single) + 1
            (parentBranch, idx)

          } { b =>
            val idx = handler.children(b).toIndexedSeq.size
            (b, idx)
          }

        case _ =>
          val parentBranch  = rootView.branchH()
          val idx           = handler.children(parentBranch).toIndexedSeq.size
          (parentBranch, idx)
      }
    }

    def elemAdded(parent: VBranchL, idx: Int, elem: Node, refresh: Boolean)
                 (implicit tx: S#Tx): VNode = {

      val edit = didInsert.swap(false)(tx.peer)
      if (DEBUG) println(s"elemAdded($parent, $idx $elem); marked? $edit")

      def addView(id: S#Id, v: VNode): Unit = {
        mapViews.put(id, v)

        if (refresh) {
          deferTx {
            treeModel.elemAdded(parent, idx, v)
            if (edit) {
              val path    = treeModel.getPath(v)
              val row     = t.getRowForPath(path)
              val column  = t.hierarchicalColumn
              t.requestFocus()
              t.changeSelection(row, column, toggle = false, extend = false)
              t.editCellAt     (row, column)

              // TODO: this doesn't work yet, it doesn't activate the cursor, see TreeTable issue #12

              //            val tej = t.peer.getEditorComponent
              //            EventQueue.invokeLater(new Runnable {
              //              def run(): Unit = tej.requestFocus()
              //            })
              //            tej match {
              //              case tf: javax.swing.JTextField => ...
              //              case _ =>
              //            }

              // })
              // t.editCellAt(row, column)
            }
          }
        } else {
          treeModel.elemAddedNoRefresh(parent, idx, v)
        }
      }

      val data  = handler.data(elem)
      // println(s"Data = $data")
      val id    = elem.id // handler.nodeId(elem)
      val src   = tx.newHandle(elem)(nodeSerializer)

      val observer = handler.observe(elem, processUpdateFun)

      val view = handler.branchOption(elem).fold[VNode] {
        val _v = new NodeViewImpl.Leaf(parent, data, src, observer)
        addView(id, _v)
        _v
      } { branch =>
        val _v = new NodeViewImpl.BranchImpl(parent, tx.newHandle(branch), data, src, observer)
        addView(id, _v)
        mapBranches.put(branch.id, _v)
        val it = handler.children(branch)
        it.toList.zipWithIndex.foreach { case (c, ci) =>
          elemAdded(_v, ci, c, refresh = refresh)
        }
        _v
      }

      //      val obs = elem.changed.react { implicit tx => upd =>
      //        processUpdate(view, upd)
      //      }
      //      view.observer = obs
      view
    }

    def elemRemoved(parent: VBranchL, idx: Int, elem: Node)(implicit tx: S#Tx): Unit = {
      if (DEBUG) println(s"elemRemoved($parent, $idx)")

      // parent.observer.dispose()
      val id = elem.id // handler.nodeId(elem)
      handler.branchOption(elem).foreach { branch =>
        val it  = handler.children(branch)
        val bid = branch.id
        mapBranches.get(bid).fold(warnNoView(elem)) { fv =>
          it.toIndexedSeq.zipWithIndex.reverse.foreach { case (c, ci) =>
            elemRemoved(fv, ci, c)
          }
        }
        mapBranches.remove(bid)
      }
      mapViews.get(id).fold(warnNoView(elem)) { view =>
        mapViews.remove(id)
        view.dispose()
      }

      deferTx {
        treeModel.elemRemoved(parent, idx)
      }
    }

    // this is wrong: mixes GUI calls with txn:
    //    def insertionPoint()(implicit tx: S#Tx): (Node, Int) = {
    //      val pOpt = treeTable.selection.paths.headOption.flatMap {
    //        case path @ init :+ last =>
    //          last match {
    //            case fv: VBranch if treeTable.isExpanded(path) => Some(last.modelData() -> 0)
    //            case _ =>
    //              init match {
    //                case _ :+ _parent =>
    //                  _parent.modelData() match {
    //                    case TreeLike.IsBranch(b) => Some(b -> (b.indexOf(child) + 1))
    //                    case _ => None
    //                  }
    //
    //                case _ => None
    //              }
    //          }
    //        case _ => None
    //      }
    //      pOpt.getOrElse {
    //        val root = rootView.modelData()
    //        root -> handler.children(root).fold(0)(_.toIndexedSeq.size)
    //      }
    //    }

//    private def warnNoBranchView(view: VNodeL): Unit =
//      println(s"Warning: should find a branch view but got $view:")

    private def warnNoView(node: Any): Unit =
      println(s"Warning: should find a view for node $node")

//    private def warnRoot(): Unit =
//      println("Warning: should not refer to root node")

    def processUpdate(mUpd0: Update)(implicit tx: S#Tx): Unit = {
      // val mUpd0 = handler.mapUpdate(/* view0.modelData(), */ update0 /* , view0.renderData */)

      def withParentView(parent: Branch)(fun: VBranchL => Unit): Unit = {
        val parentId = parent.id // handler.nodeId(parent)
        mapBranches.get(parentId).fold(warnNoView(parent))(fun.apply)
      }

      mUpd0 match /* .foreach */ {
        case TreeTableView.NodeAdded(parent, idx, child) =>
          withParentView(parent)(elemAdded(_, idx, child, refresh = true))

        case TreeTableView.NodeRemoved(parent, idx, child) =>
          withParentView(parent)(elemRemoved(_, idx, child))

        case TreeTableView.NodeChanged(node) =>
          val nodeId = node.id // handler.nodeId(node)
          mapViews.get(nodeId).fold(warnNoView(node)) { view =>
            deferTx {
              treeModel.elemUpdated(view)
            }
          }
      }
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      // observer.dispose()
      def disposeChildren(node: VNodeL): Unit = {
        node match {
          case b: VBranchL =>
            b.children.foreach(disposeChildren)
          case _ =>
        }
        node.dispose()
      }
      disposeChildren(rootView)
      mapViews   .dispose()
      mapBranches.dispose()
    }

    protected def guiInit(): Unit = {
      requireEDT()

      val tcm = new TreeColumnModel[VNodeL] {
        // private val peer = handler.columns

        def getValueAt(r: VNodeL, column: Int): Any = r

        def setValueAt(value: Any, r: VNodeL, column: Int): Unit = ()
        //          r match {
        //          case node: VNode  =>
        //            //            val ex = new Exception
        //            //            ex.fillInStackTrace()
        //            //            ex.printStackTrace()
        //            // println(s"setValueAt($value, $r, $column")
        //            // peer.setValueAt(value, node.renderData, column)
        //          case _            => throw new IllegalStateException(s"Trying to alter $r")
        //        }

        def getColumnName (column: Int): String   = {
          // peer.getColumnName (column)
          handler.columnNames(column)
        }

        def getColumnClass(column: Int): Class[_] = classOf[AnyRef] // classOf[V] // peer.getColumnClass(column)

        def columnCount: Int = handler.columnNames.size

        def isCellEditable(r: VNodeL, column: Int): Boolean = r match {
          case node: VNode  => handler.isEditable(node.renderData, column)
          case _            => false
        }

        def hierarchicalColumn: Int = 0 // peer.hierarchicalColumn
      }

      t = new TreeTable(treeModel, tcm: TreeColumnModel[VNodeL])
      t.rootVisible = false
      val r = new DefaultTableCellRenderer with TreeTableCellRenderer {
        // private lazy val lb = new Label
        private lazy val wrapSelf = Component.wrap(this)

        override def getRendererComponent(treeTable: TreeTable[_, _], value: Any, row: Int, column: Int,
                                 state: TreeTableCellRenderer.State): Component = {
          // XXX TODO: this shows that somehow the transformed value from the table column model is used initially:
          // println(s"getRendererComponent; value = $value (${value.getClass}), row = $row, col = $column")
          value match {
            // case _: VRoot =>
            //   wrapSelf
            case b: VNode =>
              handler.renderer(view, b /* .renderData */, row = row, column = column, state = state)
            case _ =>
              wrapSelf // super.getRendererComponent(treeTable, value, row, column, state)
          }
        }
      }

      val rj: DefaultTableCellRenderer = new DefaultTableCellRenderer with j.TreeTableCellRenderer {
        def getTreeTableCellRendererComponent(treeTable: j.TreeTable, value: Any, selected: Boolean,
                                              hasFocus: Boolean, row: Int, column: Int): java.awt.Component = {
          val state = TreeTableCellRenderer.State(selected = selected, focused = hasFocus, tree = None)
          r.getRendererComponent(t, value, row, column, state).peer
        }

        def getTreeTableCellRendererComponent(treeTable: j.TreeTable, value: Any, selected: Boolean,
                                              hasFocus: Boolean, row: Int, column: Int,
                                              expanded: Boolean, leaf: Boolean): java.awt.Component = {
          val treeState = TreeTableCellRenderer.TreeState(expanded = expanded, leaf = leaf)
          val state = TreeTableCellRenderer.State(selected = selected, focused = hasFocus, tree = Some(treeState))
          r.getRendererComponent(t, value, row, column, state).peer
        }
      }

      //      val ej = new DefaultTreeTableCellEditor(new javax.swing.JTextField()) {
      //        override def getTreeTableCellEditorComponent(treeTable: j.TreeTable, value: Any, selected: Boolean,
      //                                                     row: Int, column: Int): java.awt.Component =
      //          value match {
      //            case b: VNode => handler.editor(view, b.renderData, row = row, column = column, selected = selected).peer
      //            case _ => super.getTreeTableCellEditorComponent(treeTable, value, selected, row, column)
      //            //          val v1 = value match {
      //            //            case b: VBranch  =>
      //            //              // println(s"branchRenderer(${b.data}, row = $row)")
      //            //              b.renderData
      //            //            case l: VLeaf     =>
      //            //              // println(s"leafRenderer(${l.data}, row = $row)")
      //            //              l.renderData
      //            //            case _ => value
      //            //          }
      //            //          super.getTreeTableCellEditorComponent(treeTable, v1, selected, row, column)
      //          }
      //
      //        override def getTreeTableCellEditorComponent(treeTable: j.TreeTable, value: Any, selected: Boolean,
      //                                                     row: Int, column: Int, expanded: Boolean,
      //                                                     leaf: Boolean): java.awt.Component =
      //          getTreeTableCellEditorComponent(treeTable, value, selected, row, column)
      //      }

      val ej: TreeTableCellEditor with TableCellEditor = new TreeTableCellEditor with TableCellEditor {
        private var currentEditor = Option.empty[CellEditor]

        def getTreeTableCellEditorComponent(treeTable: j.TreeTable, value: Any, selected: Boolean,
                                                     row: Int, column: Int): java.awt.Component = {
          val res = value match {
            case b: VNode => handler.editor(view, b /* .renderData */, row = row, column = column, selected = selected)
            case _ => throw new IllegalStateException(s"Not a node: $value")
          }
          currentEditor = Some(res._2)
          res._1.peer
        }

        def getTreeTableCellEditorComponent(treeTable: j.TreeTable, value: Any, selected: Boolean,
                                                     row: Int, column: Int, expanded: Boolean,
                                                     leaf: Boolean): java.awt.Component =
          getTreeTableCellEditorComponent(treeTable, value, selected, row, column)

        def getTableCellEditorComponent(table: JTable, value: Any, selected: Boolean, row: Int,
                                        column: Int): awt.Component = {
          getTreeTableCellEditorComponent(treeTable.peer, value, selected, row, column)
        }

        def addCellEditorListener(l: CellEditorListener): Unit = currentEditor.foreach { ed =>
          ed.addCellEditorListener(l)
        }

        def getCellEditorValue: AnyRef = currentEditor.map(_.getCellEditorValue).orNull

        def shouldSelectCell(e: java.util.EventObject): Boolean = currentEditor.exists(_.shouldSelectCell(e))

        // cf, https://community.oracle.com/thread/2140909?start=0&tstart=0
        def isCellEditable(e: java.util.EventObject): Boolean = e match {
          case m: java.awt.event.MouseEvent => m.getClickCount == 2
          case _ => true
        }

        def stopCellEditing(): Boolean = currentEditor.exists(_.stopCellEditing())

        def removeCellEditorListener(l: CellEditorListener): Unit = currentEditor.foreach { ed =>
          ed.removeCellEditorListener(l)
        }

        def cancelCellEditing(): Unit = currentEditor.foreach(_.cancelCellEditing())
      }

      val cm = t.peer.getColumnModel
      for (col <- handler.columnNames.indices) {
        // assert(r.isInstanceOf[TreeTableCellRenderer])
        val c = cm.getColumn(col)
        c.setCellRenderer(rj)
        c.setCellEditor  (ej)
      }

      t.listenTo(t.selection)
      t.reactions += {
        case _: TreeTableSelectionChanged[_, _] =>  // this crappy untyped event doesn't help us at all
          dispatch(TreeTableView.SelectionChanged)
        // println(s"selection: $e")
        // dispatch(BranchView.SelectionChanged(view, selection))
        // case e => println(s"other: $e")
      }
      t.showsRootHandles  = true
      t.expandPath(TreeTable.Path(treeModel.root))
      t.dragEnabled       = true
      t.dropMode          = DropMode.ON_OR_INSERT_ROWS

      val scroll    = new ScrollPane(t)
      scroll.border = null
      component     = scroll
    }
  }
}