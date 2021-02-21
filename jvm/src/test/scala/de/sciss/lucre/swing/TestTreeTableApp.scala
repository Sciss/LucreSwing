package de.sciss.lucre.swing

import de.sciss.lucre.Event.Targets
import de.sciss.lucre.impl.{ObjFormat, SingleEventNode}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.TreeTableView.ModelUpdate
import de.sciss.lucre.{Copy, Cursor, Disposable, Elem, Event, Ident, IntObj, ListObj, Obj, Publisher, Pull, Txn}
import de.sciss.model.Change
import de.sciss.serial.{DataInput, DataOutput, TFormat}
import de.sciss.treetable.TreeTableCellRenderer.State
import de.sciss.treetable.j.{DefaultTreeTableCellEditor, DefaultTreeTableCellRenderer}
import javax.swing.event.{CellEditorListener, ChangeEvent}
import javax.swing.{CellEditor, JComponent, JTextField}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.{BorderPanel, Button, Component, FlowPanel}

object TestTreeTableApp extends DurableAppLike {
  private val instance = new TestTreeTableApp[T]()(system)

  protected def mkView(): Component = instance.mkView()
}
class TestTreeTableApp[T1 <: Txn[T1]]()(implicit val cursor: Cursor[T1]) { app =>
  object Node extends Obj.Type {
    final val typeId = 0x10000000

    object Update {
      case class Branch[T <: Txn[T]](branch: app.Branch[T], peer: ListObj.Update[T, Node[T], ListObj[T, Node[T]]]) extends Update[T]
      case class Leaf  [T <: Txn[T]](leaf  : app.Leaf  [T], peer: Change[Int]) extends Update[T]
    }
    trait Update[T <: Txn[T]]

    implicit def serializer[T <: Txn[T]]: Ser[T] = new Ser[T]

    class Ser[T <: Txn[T]] extends ObjFormat[T, Node[T]] {
//      def read(in: DataInput, access: S#Acc)(implicit tx: T): Node = {
//        val targets = evt.Targets.read[T](in, access)
//        read(in, access, targets)
//      }

      def tpe: Obj.Type = Node

      def read(in: DataInput, targets: Targets[T])(implicit tx: T): Node[T] with Event.Node[T] =
        in.readByte() match {
          case 0 =>
            val peer = ListObj.Modifiable.read[T, Node[T]](in)
            new Branch(targets, peer)
          case 1 =>
            val peer = IntObj.readVar[T](in)
            new Leaf(targets, peer)
        }

//      def write(node: Node, out: DataOutput): Unit = node.write(out)
    }

    def readIdentifiedObj[T <: Txn[T]](in: DataInput)(implicit tx: T): Obj[T] = {
      val targets = Targets.read[T](in)
      serializer[T].read(in, targets)
    }
  }

  sealed trait Node[T <: Txn[T]] extends Obj[T]
    with Publisher[T, Node.Update[T]] with Event.Node[T] {

    def tpe: Obj.Type = Node

    // def reader = Node.Ser
    def branchOption: Option[Branch[T]]
  }

  Node.init()

  class Branch[T <: Txn[T]](val targets: Targets[T], val children: ListObj.Modifiable[T, Node[T]])
    extends Node[T]
    with SingleEventNode[T, Node.Update[T]]
    /* with evt.impl.MappingGenerator[T, Node.Update, expr.List.Update[T, Node], Node] */ { branch =>

    // def inputEvent = children.changed

    def branchOption = Some(this)

    object changed extends Changed
      // with evt.impl.RootGenerator[T, Node.Update[T]]
    {
      private[lucre] def pullUpdate(pull: Pull[T])(implicit tx: T): Option[Node.Update[T]] = {
        pull(children.changed).map { peer =>
          Node.Update.Branch(branch, peer)
        }
      }
    }

    def copy[Out <: Txn[Out]]()(implicit tx: T, txOut: Out, context: Copy[T, Out]): Elem[Out] = {
      type ListAux[~ <: Txn[~]] = ListObj.Modifiable[~, Node[~]]
      new Branch[Out](Targets[Out](), context[ListAux](children)) // .connect()
    }

    def connect()(implicit tx: T): this.type = {
      children.changed ---> changed
      this
    }

    private def disconnect()(implicit tx: T): Unit =
      children.changed -/-> changed

    def writeData(out: DataOutput): Unit = {
      out.writeByte(0)
      children.write(out)
    }

    def disposeData()(implicit tx: T): Unit = {
      disconnect()
      children.dispose()
    }

//    def foldUpdate(generated: Option[Node.Update], input: expr.List.Update[T, Node])
//                  (implicit tx: T): Option[Node.Update] = Some(Node.Update.Branch(this, input))
  }
  class Leaf[T <: Txn[T]](val targets: Targets[T], val expr: IntObj.Var[T])
    extends Node[T]
    with SingleEventNode[T, Node.Update[T]]
    /* with evt.impl.MappingGenerator[T, Node.Update, Change[Int], Node] */ { leaf =>

    def branchOption: Option[Branch[T]] = None

    def copy[Out <: Txn[Out]]()(implicit tx: T, txOut: Out, context: Copy[T, Out]): Elem[Out] =
      new Leaf(Targets[Out](), context(expr)) // .connect()

    def connect()(implicit tx: T): this.type = {
      expr.changed ---> this.changed
      this
    }

    private def disconnect()(implicit tx: T): Unit =
      expr.changed -/-> this.changed

    def writeData(out: DataOutput): Unit = {
      out.writeByte(1)
      expr.write(out)
    }

    def disposeData()(implicit tx: T): Unit = {
      disconnect()
      expr.dispose()
    }

    object changed extends Changed {
      def pullUpdate(pull: Pull[T])(implicit tx: T): Option[Node.Update[T]] =
        pull(expr.changed).map(Node.Update.Leaf[T](leaf, _))
    }

//    def foldUpdate(generated: Option[Node.Update], input: Change[Int])
//                  (implicit tx: T): Option[Node.Update] = Some(Node.Update.Leaf(this, input))
  }

  implicit def branchSerializer[T <: Txn[T]]: TFormat[T, Branch[T]] =
    new BranchSer[T]

  private class BranchSer[T <: Txn[T]] extends TFormat[T, Branch[T]] {
    def readT(in: DataInput)(implicit tx: T): Branch[T] = {
      val targets = Targets.read[T](in)
      read(in, targets)
    }

    def read(in: DataInput, targets: Targets[T])(implicit tx: T): Branch[T] with Event.Node[T] =
      in.readByte() match {
        case 0 =>
          val peer = ListObj.Modifiable.read[T, Node[T]](in)
          new Branch[T](targets, peer)
      }

    def write(node: Branch[T], out: DataOutput): Unit = node.write(out)
  }

  object Data {
    case object Branch extends Data
    class Leaf(var value: Int) extends Data {
      override def toString = s"Data.Leaf($value)"
    }
  }
  sealed trait Data

  class Handler[T <: Txn[T]](implicit cursor: Cursor[T])
    extends TreeTableView.Handler[T, Node[T], Branch[T], Data] {

    var view: TreeTableView[T, Node[T], Branch[T], Data] = _

    def nodeId(node: Node[T]): Ident[T] = node.id

    def children(b: Branch[T])(implicit tx: T): Iterator[Node[T]] = b.children.iterator

    def branchOption(node: Node[T]): Option[Branch[T]] = node.branchOption

    def observe(n: Node[T], dispatch: T => ModelUpdate[Node[T], Branch[T]] => Unit)
               (implicit tx: T): Disposable[T] = n.changed.react { implicit tx => upd =>
      val m = mapUpdate(upd)
      m.foreach(dispatch(tx)(_))
    }

    private def mapUpdate(upd: Node.Update[T])(implicit tx: T): Vec[ModelUpdate[Node[T], Branch[T]]] =
      upd match {
        case Node.Update.Branch(parent, peer) =>
          peer.changes.flatMap {
            case ListObj.Added  (idx, elem) => Vec(TreeTableView.NodeAdded  (parent, idx, elem))
            case ListObj.Removed(idx, elem) => Vec(TreeTableView.NodeRemoved(parent, idx, elem))
// ELEM
//            case expr.List.Element(elem, eUpd) =>
//              mapUpdate(eUpd)
          }

        case Node.Update.Leaf(l, Change(_, now)) =>
          view.nodeView(l).fold[Vec[ModelUpdate[Node[T], Branch[T]]]](Vec.empty) { nv =>
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

    private var editingNode = Option.empty[TreeTableView.NodeView[T, Node[T], Branch[T], Data]]

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
                  case l: Leaf[T] => l.expr() = IntObj.newConst[T](i)
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

    def renderer(tt: TreeTableView[T, Node[T], Branch[T], Data], node: TreeTableView.NodeView[T, Node[T], Branch[T], Data],
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

    def editor(tt: TreeTableView[T, Node[T], Branch[T], Data], node: TreeTableView.NodeView[T, Node[T], Branch[T], Data],
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

    def data(node: Node[T])(implicit tx:  T): Data = node match {
      case _: Branch[T]  => Data.Branch
      case l: Leaf  [T]  => new Data.Leaf(l.expr.value)
    }
  }

  // lazy val h = new Handler[T](view)

  def newBranch[T <: Txn[T]]()(implicit tx: T): Branch[T] = {
    val li    = ListObj.Modifiable[T, Node]
    val tgt   = Targets[T]()
    new Branch[T](tgt, li).connect()
  }

  def newLeaf[T <: Txn[T]]()(implicit tx: T): Leaf[T] = {
    val ex    = IntObj.newVar[T](IntObj.newConst((math.random() * 100).toInt))
    val tgt   = Targets[T]()
    new Leaf(tgt, ex).connect()
  }

  private lazy val (treeH, view: TreeTableView[T1, Node[T1], Branch[T1], Data]) = cursor.step { implicit tx =>
    val root  = newBranch[T1]()
    val h     = new Handler[T1]
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

  private def add(child: Node[T1])(implicit tx: T1): Unit = {
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
      val parent = childView.parentView.fold[Node[T1]](treeH())(_.modelData())
      parent match {
        case b: Branch[T1] => Some(b -> childView.modelData())
        case _             => None
      }
    }
    toRemove.reverse.foreach { case (parent, child) =>
      parent.children.remove(child)
    }
  }

  private def modifyAction(): Unit = cursor.step { implicit tx =>
    view.selection.foreach { v =>
      v.modelData() match {
        case l: Leaf[T1] => l.expr() = IntObj.newConst[T1]((math.random() * 100).toInt)
        case _ =>
      }
    }
  }

  protected def mkView(): Component = new BorderPanel { me =>
    me.add(view.component, BorderPanel.Position.Center)
    me.add(new FlowPanel(
      Button("Add Branch")(addBranchAction()),
      Button("Add Leaf"  )(addLeafAction()),
      Button("Modify"    )(modifyAction()),
      Button("Remove"    )(removeAction())
    ), BorderPanel.Position.South)
  }
}
