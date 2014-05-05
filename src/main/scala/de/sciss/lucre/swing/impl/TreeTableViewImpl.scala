/*
 *  TreeTableViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing
package impl

import scala.swing.{ScrollPane, Component}
import de.sciss.lucre.stm.{Source, Disposable, IdentifierMap}
import de.sciss.model.impl.ModelImpl
import javax.swing.DropMode
import de.sciss.treetable.{j, TreeTableSelectionChanged, TreeTableCellRenderer, TreeColumnModel, AbstractTreeModel, TreeTable}
import de.sciss.lucre.{event => evt}
import evt.Sys
import TreeTableView.Handler
import javax.swing.table.DefaultTableCellRenderer
import de.sciss.lucre.stm
import collection.breakOut
import scala.concurrent.stm.TxnLocal
import scala.annotation.tailrec
import de.sciss.treetable.j.DefaultTreeTableCellEditor
import scala.collection.immutable.{IndexedSeq => Vec}
import de.sciss.serial.Serializer
import java.awt.EventQueue

object TreeTableViewImpl {
  private final val DEBUG = false

  private object NodeViewImpl {
    sealed trait Base[S <: Sys[S], Node, Data] {
      def isLeaf: Boolean
      def numChildren: Int
      def parentOption1: Option[BranchOrRoot[S, Node, Data]]
    }

    sealed trait BranchOrRoot[S <: Sys[S], Node, Data] extends Base[S, Node, Data] {
      var children = Vec.empty[NodeViewImpl[S, Node, Data]]
      def isLeaf = false
      def numChildren = children.size
    }

    class Branch[S <: Sys[S], Node, Data](val parent: BranchOrRoot[S, Node, Data],
                                          var renderData: Data,
                                          val modelData: stm.Source[S#Tx, Node])
      extends BranchOrRoot[S, Node, Data] with NodeViewImpl[S, Node, Data] {

      override def toString = s"Branch($modelData, $renderData)"
    }

    class Leaf[S <: Sys[S], Node, Data](val parent: BranchOrRoot[S, Node, Data],
                                        var renderData: Data,
                                        val modelData: stm.Source[S#Tx, Node])
      extends NodeViewImpl[S, Node, Data] {

      def isLeaf = true
      def numChildren = 0

      override def toString = s"Leaf($modelData, $renderData)"
    }

    //    sealed trait OrRoot[S <: Sys[S], Node, Data] {
    //      def parentOption: Option[NodeViewImpl.Branch[S, Node, Data]]
    //      var observer: Disposable[S#Tx] = _
    //      def numChildren: Int
    //    }

    class Root[S <: Sys[S], Node, Branch, Data](val rootH: stm.Source[S#Tx, Branch])
      extends BranchOrRoot[S, Node, Data] {

      def parentOption1: Option[NodeViewImpl.BranchOrRoot[S, Node, Data]] = None
    }
  }
  private sealed trait NodeViewImpl[S <: Sys[S], Node, Data]
    extends NodeViewImpl.Base[S, Node, Data] with TreeTableView.NodeView[S, Node, Data] {

    def parent: NodeViewImpl.BranchOrRoot[S, Node, Data]

    var renderData: Data

    def parentOption1: Option[NodeViewImpl.BranchOrRoot[S, Node, Data]] = Some(parent)

    def parentOption: Option[NodeViewImpl.Branch[S, Node, Data]] = parent match {
      case b: NodeViewImpl.Branch[S, Node, Data] => Some(b)
      case _ => None
    }

    def numChildren: Int
  }

  def apply[S <: Sys[S], Node, Branch <: evt.Publisher[S, U], U, Data](root: Branch,
       handler: Handler[S, Node, Branch, U, Data])(implicit tx: S#Tx, nodeSerializer: Serializer[S#Tx, S#Acc, Node],
                                                   branchSerializer: Serializer[S#Tx, S#Acc, Branch])
      : TreeTableView[S, Node, Branch, Data] = {
    val _handler = handler
    new Impl[S, Node, Branch, U, Data] {
      val mapViews  = tx.newInMemoryIDMap[VNode]  // node IDs to renderers
      val handler   = _handler
      val rootView  = new NodeViewImpl.Root[S, Node, Branch, Data](tx.newHandle(root))
      handler.children(root).toList.zipWithIndex.foreach { case (c, ci) =>
        elemAdded(rootView, ci, c, refresh = false)
      }
      val observer = root.changed.react { implicit tx => upd =>
        processUpdate(upd)
      }

      // val rootView  = elemAdded(None, -1, root, refresh = false)

      deferTx {
        guiInit()
      }
    }
  }

  private abstract class Impl[S <: Sys[S], Node, Branch <: evt.Publisher[S, U], U, Data](
      implicit nodeSerializer: Serializer[S#Tx, S#Acc, Node])
    extends ComponentHolder[Component] with TreeTableView[S, Node, Branch, Data] with ModelImpl[TreeTableView.Update] {
    view =>

    type VBranch  = NodeViewImpl.Branch       [S, Node, Data]
    type VBranchL = NodeViewImpl.BranchOrRoot [S, Node, Data]
    type VLeaf    = NodeViewImpl.Leaf         [S, Node, Data]
    type VNode    = NodeViewImpl              [S, Node, Data]
    type VNodeL   = NodeViewImpl.Base         [S, Node, Data]
    type VRoot    = NodeViewImpl.Root         [S, Node, Branch, Data]
    type TPath    = TreeTable.Path[VBranch]
    type NodeView = VNode // alias in the interface

    protected def rootView: VRoot
    protected def mapViews: IdentifierMap[S#ID, S#Tx, VNode]
    protected def observer: Disposable[S#Tx]
    protected def handler : Handler[S, Node, Branch, U, Data]

    private val didInsert = TxnLocal(false)

    private class ElementTreeModel extends AbstractTreeModel[VNodeL] {
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

      def getPath(node: VNode): TreeTable.Path[VNode] = {
        @tailrec def loop(n: VNode, res: TreeTable.Path[VNode]): TreeTable.Path[VNode] = {
          val res1 = n +: res
          n.parentOption match {
            case Some(p)  => loop(p, res1)
            case _        => res1
          }
        }
        loop(node, Vec.empty)
      }

      def valueForPathChanged(path: TreeTable.Path[VNodeL], newValue: VNodeL): Unit =
        println(s"valueForPathChanged($path, $newValue)")

      def elemAdded(parent: VBranchL, idx: Int, view: VNode): Unit = {
        if (DEBUG) println(s"model.elemAdded($parent, $idx, $view)")
        val g       = parent  // Option.getOrElse(_root)
        require(idx >= 0 && idx <= g.children.size)
        g.children  = g.children.patch(idx, Vector(view), 0)
        fireNodesInserted(view)
      }

      def elemRemoved(parent: VBranchL, idx: Int): Unit = {
        if (DEBUG) println(s"model.elemRemoved($parent, $idx)")
        require(idx >= 0 && idx < parent.children.size)
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

    private var _model: ElementTreeModel  = _
    private var t: TreeTable[VNodeL, TreeColumnModel[VNodeL]] = _

    def treeTable: TreeTable[_, _] = t

    // def selection: List[VNode] = t.selection.paths.flatMap(_.lastOption)(breakOut)

    def selection: List[VNode] = t.selection.paths.collect {
      case init :+ (last: VNode) => last
    } (breakOut)

    def markInsertion()(implicit tx: S#Tx): Unit = didInsert.update(true)(tx.peer)

    def insertionPoint()(implicit tx: S#Tx): (Branch, Int) = {
      if (!EventQueue.isDispatchThread) throw new IllegalStateException("Must be called on the EDT")
      selection match {
        case singleView :: Nil =>
          val single = singleView.modelData()
          handler.branchOption(single).fold[(Branch, Int)] {
            val parentView    = singleView.parent // Option.getOrElse(rootView)
            val parentBranch  = parentView match {
              case b: VBranch => handler.branchOption(b.modelData()).getOrElse(throw new IllegalStateException())
              case r: VRoot   => r.rootH()
            }
            val idx = handler.children(parentBranch).toIndexedSeq.indexOf(single)
            (parentBranch, idx)

          } { b =>
            val idx = handler.children(b).toIndexedSeq.size
            (b, idx)
          }

        case _ =>
          val parentBranch  = rootView.rootH()
          val idx           = handler.children(parentBranch).toIndexedSeq.size
          (parentBranch, idx)
      }
    }

    def elemAdded(parent: VBranchL, idx: Int, elem: Node, refresh: Boolean)
                 (implicit tx: S#Tx): VNode = {

      val edit = didInsert.swap(false)(tx.peer)
      if (DEBUG) println(s"elemAdded($parent, $idx $elem); marked? $edit")

      def addView(id: S#ID, v: VNode): Unit = {
        mapViews.put(id, v)

        if (refresh) deferTx {
          _model.elemAdded(parent, idx, v)
          if (edit) {
            val path = _model.getPath(v)
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
      }

      val data  = handler.data(elem)
      val id    = handler.nodeID(elem)
      val src   = tx.newHandle(elem)(nodeSerializer)

      val view = handler.branchOption(elem).fold[VNode] {
        val _v = new NodeViewImpl.Leaf(parent, data, src)
        addView(id, _v)
        _v
      } { branch =>
        val _v = new NodeViewImpl.Branch(parent, data, src)
        addView(id, _v)
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
      val id = handler.nodeID(elem)
      handler.branchOption(elem).foreach { branch =>
        val it = handler.children(branch)
        mapViews.get(id) match {
          case Some(fv: VBranchL) =>
            it.toIndexedSeq.zipWithIndex.reverse.foreach { case (c, ci) =>
              elemRemoved(fv, ci, c)
            }
          case Some(other) => warnNoBranchView(other)
          case _ => warnNoView(elem)
        }
      }
      mapViews.remove(id)
      deferTx {
        _model.elemRemoved(parent, idx)
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

    private def warnNoBranchView(view: VNodeL): Unit =
      println(s"Warning: should find a branch view but got $view:")

    private def warnNoView(node: Node): Unit =
      println(s"Warning: should find a view for node $node")

    private def warnRoot(): Unit =
      println("Warning: should not refer to root node")

    def processUpdate(update0: U)(implicit tx: S#Tx): Unit = {
      /* @tailrec */ def loop(mUpd: TreeTableView.ModelUpdate[Node, Data], view: VNodeL): Unit =
        mUpd match {
          case TreeTableView.NodeAdded(idx, child) =>
            val parent = view match {
              case vb: VBranchL  => vb
              case _ =>
                throw new IllegalStateException(s"Not a branch: $view")
            }
            elemAdded(parent, idx, child, refresh = true)
          case TreeTableView.NodeRemoved(idx, child) =>
            view match {
              case parent: VBranchL  =>
                elemRemoved(parent, idx, child)
              case _            =>
                warnNoBranchView(view)
            }
          case TreeTableView.NodeChanged(child, newData) =>
            val childID = handler.nodeID(child)
            mapViews.get(childID).fold(warnNoView(child)) { childView =>
              if (newData != childView.renderData) {
                childView.renderData = newData
                deferTx {
                  _model.elemUpdated(childView)
                }
              }
            }
          case TreeTableView.Nested(idx, child, u1) =>
            val view1 = mapViews.get(handler.nodeID(child))
              .getOrElse(throw new IllegalStateException(s"No view for $child"))
            loop(u1, view1)
        }

      val mUpd0 = handler.update(/* view0.modelData(), */ update0 /* , view0.renderData */)
      mUpd0.foreach(u0 => loop(u0, rootView))
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      observer.dispose()
      //      def disposeChildren(node: Node): Unit = {
      //        val nodeID        = handler.nodeID(node)
      //        val nodeViewOpt   = mapViews.get(nodeID)
      //        mapViews.remove(nodeID)
      //        nodeViewOpt.foreach { nodeView =>
      //          nodeView.observer.dispose()
      //        }
      //        handler.branchOption(node).foreach { branch =>
      //          val it = handler.children(branch)
      //          it.foreach { child =>
      //            disposeChildren(child)
      //          }
      //        }
      //      }
      //      disposeChildren(rootView.modelData())
      mapViews.dispose()
    }

    protected def guiInit(): Unit = {
      requireEDT()

      _model = new ElementTreeModel

      val tcm = new TreeColumnModel[VNodeL] {
        private val peer = handler.columns

        def getValueAt(r: VNodeL, column: Int): Any = r

        def setValueAt(value: Any, r: VNodeL, column: Int): Unit = r match {
          case node: VNode  => peer.setValueAt(value, node.renderData, column)
          case _            => throw new IllegalStateException(s"Trying to alter $r")
        }

        def getColumnName (column: Int): String   = peer.getColumnName (column)
        def getColumnClass(column: Int): Class[_] = classOf[AnyRef] // classOf[V] // peer.getColumnClass(column)

        def columnCount: Int = peer.columnCount

        def isCellEditable(r: VNodeL, column: Int): Boolean = r match {
          case node: VNode  => peer.isCellEditable(node.renderData, column)
          case _            => false
        }

        def hierarchicalColumn: Int = peer.hierarchicalColumn
      }

      t = new TreeTable(_model, tcm: TreeColumnModel[VNodeL])
      t.rootVisible = false
      val r = new DefaultTableCellRenderer with TreeTableCellRenderer {
        // private lazy val lb = new Label
        private lazy val wrapSelf = Component.wrap(this)

        def getRendererComponent(treeTable: TreeTable[_, _], value: Any, row: Int, column: Int,
                                 state: TreeTableCellRenderer.State): Component = {
          value match {
            case _: VRoot =>
              wrapSelf
            case b: VNode =>
              handler.renderer(view, b.renderData, row = row, column = column, state = state)
            case _ =>
              wrapSelf
          }
        }
      }

      val rj = new DefaultTableCellRenderer with j.TreeTableCellRenderer {
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

      val ej = new DefaultTreeTableCellEditor(new javax.swing.JTextField()) {
        override def getTreeTableCellEditorComponent(treeTable: j.TreeTable, value: Any, selected: Boolean,
                                                     row: Int, column: Int): java.awt.Component = {
          val v1 = value match {
            case b: VBranch  =>
              // println(s"branchRenderer(${b.data}, row = $row)")
              b.renderData
            case l: VLeaf     =>
              // println(s"leafRenderer(${l.data}, row = $row)")
              l.renderData
            case _ => value
          }
          super.getTreeTableCellEditorComponent(treeTable, v1, selected, row, column)
        }

        override def getTreeTableCellEditorComponent(treeTable: j.TreeTable, value: Any, selected: Boolean,
                                                     row: Int, column: Int, expanded: Boolean,
                                                     leaf: Boolean): java.awt.Component = {
          val v1 = value match {
            case b: VBranch  =>
              // println(s"branchRenderer(${b.data}, row = $row)")
              b.renderData
            case l: VLeaf     =>
              // println(s"leafRenderer(${l.data}, row = $row)")
              l.renderData
            case _ => value
          }
          super.getTreeTableCellEditorComponent(treeTable, v1, selected, row, column, expanded, leaf)
        }
      }

      val cm = t.peer.getColumnModel
      for (col <- 0 until handler.columns.columnCount) {
        // assert(r.isInstanceOf[TreeTableCellRenderer])
        val c = cm.getColumn(col)
        c.setCellRenderer(rj)
        c.setCellEditor  (ej)
      }

      t.listenTo(t.selection)
      t.reactions += {
        case e: TreeTableSelectionChanged[_, _] =>  // this crappy untyped event doesn't help us at all
          dispatch(TreeTableView.SelectionChanged)
        // println(s"selection: $e")
        // dispatch(BranchView.SelectionChanged(view, selection))
        // case e => println(s"other: $e")
      }
      t.showsRootHandles  = true
      t.expandPath(TreeTable.Path(_model.root))
      t.dragEnabled       = true
      t.dropMode          = DropMode.ON_OR_INSERT_ROWS

      val scroll    = new ScrollPane(t)
      scroll.border = null
      component     = scroll
    }
  }
}