package de.sciss.lucre.swing

import de.sciss.lucre.event.{Pull, Targets}
import de.sciss.lucre.expr.IntObj
import de.sciss.lucre.stm.impl.ObjSerializer
import de.sciss.lucre.stm.{Copy, Disposable, Elem, Obj, Sys}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.TreeTableView.ModelUpdate
import de.sciss.lucre.{stm, event => evt}
import de.sciss.model.Change
import de.sciss.serial.{DataInput, DataOutput, Serializer}
import de.sciss.treetable.TreeTableCellRenderer.State
import de.sciss.treetable.j.{DefaultTreeTableCellEditor, DefaultTreeTableCellRenderer}
import javax.swing.event.{CellEditorListener, ChangeEvent}
import javax.swing.{CellEditor, JComponent, JTextField}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.{BorderPanel, Button, Component, FlowPanel}

object TestTreeTableApp extends AppLike {
  private val instance = new TestTreeTableApp[S](system)(system)

  protected def mkView(): Component = instance.mkView()
}
class TestTreeTableApp[T <: Sys[T]](system: T)(implicit val cursor: stm.Cursor[T]) { app =>
  object Node extends Obj.Type {
    final val typeId = 0x10000000

    object Update {
      case class Branch[S <: Sys[S]](branch: app.Branch[S], peer: stm.List.Update[S, Node[S]]) extends Update[S]
      case class Leaf  [S <: Sys[S]](leaf  : app.Leaf  [S], peer: Change[Int]) extends Update[S]
    }
    trait Update[S <: Sys[S]]

    implicit def serializer[S <: Sys[S]]: Ser[S] = new Ser[S]

    class Ser[S <: Sys[S]] extends ObjSerializer[S, Node[S]] {
//      def read(in: DataInput, access: S#Acc)(implicit tx: S#Tx): Node = {
//        val targets = evt.Targets.read[S](in, access)
//        read(in, access, targets)
//      }

      def tpe: Obj.Type = Node

      def read(in: DataInput, access: S#Acc, targets: evt.Targets[S])(implicit tx: S#Tx): Node[S] with evt.Node[S] =
        in.readByte() match {
          case 0 =>
            val peer = stm.List.Modifiable.read[S, Node[S]](in, access)
            new Branch(targets, peer)
          case 1 =>
            val peer = IntObj.readVar[S](in, access)
            new Leaf(targets, peer)
        }

//      def write(node: Node, out: DataOutput): Unit = node.write(out)
    }

    def readIdentifiedObj[S <: Sys[S]](in: DataInput, access: S#Acc)(implicit tx: S#Tx): Obj[S] = {
      val targets = Targets.read[S](in, access)
      serializer[S].read(in, access, targets)
    }
  }

  sealed trait Node[S <: Sys[S]] extends Obj[S] with
    evt.Publisher[S, Node.Update[S]] with evt.Node[S] {

    def tpe: Obj.Type = Node

    // def reader = Node.Ser
    def branchOption: Option[Branch[S]]
  }

  Node.init()

  class Branch[S <: Sys[S]](val targets: evt.Targets[S], val children: stm.List.Modifiable[S, Node[S]])
    extends Node[S]
    with evt.impl.SingleNode[S, Node.Update[S]]
    /* with evt.impl.MappingGenerator[S, Node.Update, expr.List.Update[S, Node], Node] */ { branch =>

    // def inputEvent = children.changed

    def branchOption = Some(this)

    object changed extends Changed
      // with evt.impl.RootGenerator[S, Node.Update[S]]
    {
      private[lucre] def pullUpdate(pull: Pull[S])(implicit tx: S#Tx): Option[Node.Update[S]] = {
        pull(children.changed).map { peer =>
          Node.Update.Branch(branch, peer)
        }
      }
    }

    def copy[Out <: Sys[Out]]()(implicit tx: S#Tx, txOut: Out#Tx, context: Copy[S, Out]): Elem[Out] = {
      type ListAux[~ <: Sys[~]] = stm.List.Modifiable[~, Node[~]]
      new Branch[Out](Targets[Out], context[ListAux](children)) // .connect()
    }

    def connect()(implicit tx: S#Tx): this.type = {
      children.changed ---> changed
      this
    }

    private def disconnect()(implicit tx: S#Tx): Unit =
      children.changed -/-> changed

    def writeData(out: DataOutput): Unit = {
      out.writeByte(0)
      children.write(out)
    }

    def disposeData()(implicit tx: S#Tx): Unit = {
      disconnect()
      children.dispose()
    }

//    def foldUpdate(generated: Option[Node.Update], input: expr.List.Update[S, Node])
//                  (implicit tx: S#Tx): Option[Node.Update] = Some(Node.Update.Branch(this, input))
  }
  class Leaf[S <: Sys[S]](val targets: evt.Targets[S], val expr: IntObj.Var[S])
    extends Node[S]
    with evt.impl.SingleNode[S, Node.Update[S]]
    /* with evt.impl.MappingGenerator[S, Node.Update, Change[Int], Node] */ { leaf =>

    def branchOption: Option[Branch[S]] = None

    def copy[Out <: Sys[Out]]()(implicit tx: S#Tx, txOut: Out#Tx, context: Copy[S, Out]): Elem[Out] =
      new Leaf(Targets[Out], context(expr)) // .connect()

    def connect()(implicit tx: S#Tx): this.type = {
      expr.changed ---> this.changed
      this
    }

    private def disconnect()(implicit tx: S#Tx): Unit =
      expr.changed -/-> this.changed

    def writeData(out: DataOutput): Unit = {
      out.writeByte(1)
      expr.write(out)
    }

    def disposeData()(implicit tx: S#Tx): Unit = {
      disconnect()
      expr.dispose()
    }

    object changed extends Changed {
      def pullUpdate(pull: Pull[S])(implicit tx: S#Tx): Option[Node.Update[S]] =
        pull(expr.changed).map(Node.Update.Leaf(leaf, _))
    }

//    def foldUpdate(generated: Option[Node.Update], input: Change[Int])
//                  (implicit tx: S#Tx): Option[Node.Update] = Some(Node.Update.Leaf(this, input))
  }

  implicit def branchSerializer[S <: Sys[S]]: Serializer[S#Tx, S#Acc, Branch[S]] =
    new BranchSer[S]

  private class BranchSer[S <: Sys[S]] extends Serializer[S#Tx, S#Acc, Branch[S]] {
    def read(in: DataInput, access: S#Acc)(implicit tx: S#Tx): Branch[S] = {
      val targets = evt.Targets.read[S](in, access)
      read(in, access, targets)
    }

    def read(in: DataInput, access: S#Acc, targets: evt.Targets[S])(implicit tx: S#Tx): Branch[S] with evt.Node[S] =
      in.readByte() match {
        case 0 =>
          val peer = stm.List.Modifiable.read[S, Node[S]](in, access)
          new Branch[S](targets, peer)
      }

    def write(node: Branch[S], out: DataOutput): Unit = node.write(out)
  }

  object Data {
    case object Branch extends Data
    class Leaf(var value: Int) extends Data {
      override def toString = s"Data.Leaf($value)"
    }
  }
  sealed trait Data

  class Handler[S <: Sys[S]](implicit cursor: stm.Cursor[S])
    extends TreeTableView.Handler[S, Node[S], Branch[S], Data] {

    var view: TreeTableView[S, Node[S], Branch[S], Data] = _

    def nodeId(node: Node[S]): S#Id = node.id

    def children(b: Branch[S])(implicit tx: S#Tx): Iterator[Node[S]] = b.children.iterator

    def branchOption(node: Node[S]): Option[Branch[S]] = node.branchOption

    def observe(n: Node[S], dispatch: S#Tx => ModelUpdate[Node[S], Branch[S]] => Unit)
               (implicit tx: S#Tx): Disposable[S#Tx] = n.changed.react { implicit tx => upd =>
      val m = mapUpdate(upd)
      m.foreach(dispatch(tx)(_))
    }

    private def mapUpdate(upd: Node.Update[S])(implicit tx: S#Tx): Vec[ModelUpdate[Node[S], Branch[S]]] =
      upd match {
        case Node.Update.Branch(parent, peer) =>
          peer.changes.flatMap {
            case stm.List.Added  (idx, elem) => Vec(TreeTableView.NodeAdded  (parent, idx, elem))
            case stm.List.Removed(idx, elem) => Vec(TreeTableView.NodeRemoved(parent, idx, elem))
// ELEM
//            case expr.List.Element(elem, eUpd) =>
//              mapUpdate(eUpd)
          }

        case Node.Update.Leaf(l, Change(_, now)) =>
          view.nodeView(l).fold[Vec[ModelUpdate[Node[S], Branch[S]]]](Vec.empty) { nv =>
            nv.renderData match {
              case ld: Data.Leaf =>
                deferTx {
                  ld.value = now
                }
                Vec(TreeTableView.NodeChanged(l)) // , Data.Leaf(now)))
              case _ => Vec.empty
            }
          }
      }

    private var editingNode = Option.empty[TreeTableView.NodeView[S, Node[S], Branch[S], Data]]

    private lazy val r = new DefaultTreeTableCellRenderer
    private lazy val e = {
      val res = new DefaultTreeTableCellEditor(new JTextField)
      res.addCellEditorListener(new CellEditorListener {
        def editingCanceled(e: ChangeEvent): Unit = println("editingCanceled")
        def editingStopped (e: ChangeEvent): Unit = {
          println("editingStopped")
          try {
            val i = res.getCellEditorValue.toString.toInt
            editingNode.foreach { nodeView =>
              cursor.step { implicit tx =>
                nodeView.modelData() match {
                  case l: Leaf[S] => l.expr() = IntObj.newConst[S](i)
                  case _ =>
                }
              }
            }
          } catch {
            case _: NumberFormatException =>
          }
        }
      })
      res
    }

    def renderer(tt: TreeTableView[S, Node[S], Branch[S], Data], node: TreeTableView.NodeView[S, Node[S], Branch[S], Data],
                 row: Int, column: Int, state: State): Component = {
      val value = node.renderData match {
        case Data.Branch  => if (column == 0) "Branch" else ""
        case l: Data.Leaf => if (column == 0) "Leaf"   else l.value.toString
      }

      val c = state.tree.fold {
        r.getTreeTableCellRendererComponent(tt.treeTable.peer, value, state.selected, state.focused, row, column)
      } { ts =>
        r.getTreeTableCellRendererComponent(tt.treeTable.peer, value, state.selected, state.focused,
          row, column, ts.expanded, ts.leaf)
      }
      Component.wrap(c.asInstanceOf[JComponent])
    }

    def editor(tt: TreeTableView[S, Node[S], Branch[S], Data], node: TreeTableView.NodeView[S, Node[S], Branch[S], Data],
               row: Int, column: Int, selected: Boolean): (Component, CellEditor) = {
      editingNode = None
      val value = node.renderData match {
        case Data.Branch  => if (column == 0) "Branch" else "?!"
        case l: Data.Leaf => if (column == 0) "Leaf"   else {
          editingNode = Some(node)
          l.value.toString
        }
      }
      val c = e.getTreeTableCellEditorComponent(tt.treeTable.peer, value, selected, row, column)
      Component.wrap(c.asInstanceOf[JComponent]) -> e
    }

    val columnNames: Vec[String] = Vector("Foo", "Bar")

    def isEditable(data: Data, column: Int): Boolean = column == 1 && data.isInstanceOf[Data.Leaf]

    //    def update(data: Data, value: String): Unit = {
    //      println(s"update($data, $value)")
    //      data match {
    //        case l: Data.Leaf =>
    //          try {
    //            l.value = Integer.parseInt(value)
    //          } catch {
    //            case NonFatal(_) =>
    //          }
    //
    //        case _ =>
    //      }
    //    }

    def data(node: Node[S])(implicit tx:  S#Tx): Data = node match {
      case _: Branch[S]  => Data.Branch
      case l: Leaf  [S]  => new Data.Leaf(l.expr.value)
    }
  }

  // lazy val h = new Handler[T](view)

  def newBranch[S <: Sys[S]]()(implicit tx: S#Tx): Branch[S] = {
    val li    = stm.List.Modifiable[S, Node]
    val tgt   = evt.Targets[S]
    new Branch[S](tgt, li).connect()
  }

  def newLeaf[S <: Sys[S]]()(implicit tx: S#Tx): Leaf[S] = {
    val ex    = IntObj.newVar[S](IntObj.newConst((math.random * 100).toInt))
    val tgt   = evt.Targets[S]
    new Leaf(tgt, ex).connect()
  }

  private lazy val (treeH, view: TreeTableView[T, Node[T], Branch[T], Data]) = cursor.step { implicit tx =>
    val root  = newBranch[T]()
    val h     = new Handler[T]
    val _view = TreeTableView(root, h)
    h.view    = _view
    tx.newHandle(root) -> _view
  }

//  private def scramble(s: String): String = {
//    val sb  = new StringBuilder
//    var bag = (0 until s.length).toIndexedSeq
//    while (bag.nonEmpty) {
//      val i = (math.random * bag.size).toInt
//      val j = bag(i)
//      bag   = bag.patch(i, Nil, 1)
//      sb.append(s.charAt(j))
//    }
//    sb.result()
//  }

  private def add(child: Node[T])(implicit tx: T#Tx): Unit = {
    val (parent, idx) = view.insertionPoint
    parent.children.insert(idx, child)
  }

  private def addBranchAction(): Unit = cursor.step { implicit tx =>
    add(newBranch())
  }

  private def addLeafAction(): Unit = cursor.step { implicit tx =>
    add(newLeaf())
  }

  private def removeAction(): Unit = cursor.step { implicit tx =>
    val toRemove = view.selection.flatMap { childView =>
      val parent = childView.parentView.fold[Node[T]](treeH())(_.modelData())
      parent match {
        case b: Branch[T] => Some(b -> childView.modelData())
        case _            => None
      }
    }
    toRemove.reverse.foreach { case (parent, child) =>
      parent.children.remove(child)
    }
  }

  private def modifyAction(): Unit = cursor.step { implicit tx =>
    view.selection.foreach { v =>
      v.modelData() match {
        case l: Leaf[T] => l.expr() = IntObj.newConst[T]((math.random * 100).toInt)
        case _ =>
      }
    }
  }

  protected def mkView(): Component = new BorderPanel {
    add(view.component, BorderPanel.Position.Center)
    add(new FlowPanel(
      Button("Add Branch")(addBranchAction()),
      Button("Add Leaf"  )(addLeafAction()),
      Button("Modify"    )(modifyAction()),
      Button("Remove"    )(removeAction())
    ), BorderPanel.Position.South)
  }
}
