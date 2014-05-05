package de.sciss.lucre.swing

import scala.swing.{Button, FlowPanel, BorderPanel, Component}
import de.sciss.model.Change
import de.sciss.lucre.expr.Expr
import de.sciss.lucre
import de.sciss.lucre.{event => evt, expr}
import scala.concurrent.stm.{Ref, Txn}
import de.sciss.treetable.{TreeTableCellRenderer, TreeColumnModel}
import de.sciss.lucre.swing.TreeTableView.{ModelUpdate, NodeView}
import de.sciss.treetable.TreeTableCellRenderer.State
import de.sciss.lucre.swing.TestTreeTableApp.Node.Update
import de.sciss.lucre.data.Iterator
import de.sciss.serial.{DataOutput, DataInput, Serializer}
import de.sciss.treetable.j.DefaultTreeTableCellRenderer
import javax.swing.JComponent
import scala.collection.immutable.{IndexedSeq => Vec}
import scala.annotation.tailrec

object TestTreeTableApp extends AppLike {
  import lucre.expr.Int.serializer

  object Node {
    object Update {
      case class Branch(branch: TestTreeTableApp.Branch, peer: expr.List.Update[S, Node, Node.Update]) extends Update
      case class Leaf  (leaf  : TestTreeTableApp.Leaf, peer: Change[Int]) extends Update
    }
    trait Update

    implicit object Ser extends Serializer[S#Tx, S#Acc, Node] with evt.Reader[S, Node] {
      def read(in: DataInput, access: S#Acc)(implicit tx: S#Tx): Node = {
        val targets = evt.Targets.read[S](in, access)
        read(in, access, targets)
      }

      def read(in: DataInput, access: S#Acc, targets: evt.Targets[S])(implicit tx: S#Tx): Node with evt.Node[S] =
        in.readByte() match {
          case 0 =>
            val peer = expr.List.Modifiable.read[S, Node, Node.Update](in, access)
            new Branch(targets, peer)
          case 1 =>
            val peer = lucre.expr.Int.readVar[S](in, access)
            new Leaf(targets, peer)
        }

      def write(node: Node, out: DataOutput): Unit = node.write(out)
    }
  }
  sealed trait Node
    extends evt.Publisher[S, Node.Update] with evt.Node[S] {

    def reader = Node.Ser
    def branchOption: Option[Branch]
  }

  class Branch(val targets: evt.Targets[S], val children: expr.List.Modifiable[S, Node, Node.Update])
    extends Node with evt.impl.MappingGenerator[S, Node.Update, expr.List.Update[S, Node, Node.Update], Node] {

    def inputEvent = children.changed

    def branchOption = Some(this)

    def writeData(out: DataOutput): Unit = {
      out.writeByte(0)
      children.write(out)
    }

    def disposeData()(implicit tx: S#Tx): Unit =
      children.dispose()

    def foldUpdate(generated: Option[Node.Update], input: expr.List.Update[S, Node, Node.Update])
                  (implicit tx: S#Tx): Option[Node.Update] = Some(Node.Update.Branch(this, input))
  }
  class Leaf(val targets: evt.Targets[S], val expr: Expr.Var[S, Int])
    extends Node with evt.impl.MappingGenerator[S, Node.Update, Change[Int], Node] {

    def inputEvent = expr.changed

    def branchOption = None

    def writeData(out: DataOutput): Unit = {
      out.writeByte(1)
      expr.write(out)
    }

    def disposeData()(implicit tx: S#Tx): Unit =
      expr.dispose()

    def foldUpdate(generated: Option[Node.Update], input: Change[Int])
                  (implicit tx: S#Tx): Option[Node.Update] = Some(Node.Update.Leaf(this, input))
  }

  implicit object BranchSer extends Serializer[S#Tx, S#Acc, Branch] with evt.Reader[S, Branch] {
    def read(in: DataInput, access: S#Acc)(implicit tx: S#Tx): Branch = {
      val targets = evt.Targets.read[S](in, access)
      read(in, access, targets)
    }

    def read(in: DataInput, access: S#Acc, targets: evt.Targets[S])(implicit tx: S#Tx): Branch with evt.Node[S] =
      in.readByte() match {
        case 0 =>
          val peer = expr.List.Modifiable.read[S, Node, Node.Update](in, access)
          new Branch(targets, peer)
      }

    def write(node: Branch, out: DataOutput): Unit = node.write(out)
  }

  object Data {
    case object Branch extends Data
    case class Leaf(value: Int) extends Data
  }
  sealed trait Data

  private val h = new TreeTableView.Handler[S, Node, Branch, Node.Update, Data] {
    def nodeID(node: Node): S#ID = node.id

    def children(b: Branch)(implicit tx: S#Tx): Iterator[S#Tx, Node] = b.children.iterator

    def branchOption(node: Node): Option[Branch] = node.branchOption

    def update(upd: Update)(implicit tx: S#Tx): Vec[ModelUpdate[Node, Branch, Data]] =
      upd match {
        case Update.Branch(parent, peer) =>
          peer.changes.flatMap {
            case expr.List.Added  (idx, elem) => Vec(TreeTableView.NodeAdded  (parent, idx, elem))
            case expr.List.Removed(idx, elem) => Vec(TreeTableView.NodeRemoved(parent, idx, elem))
        //            case expr.List.Element(elem, Node.Update.Leaf(_, Change(_, now))) =>
        //              TreeTableView.NodeChanged(elem, Data.Leaf(now))
            case expr.List.Element(elem, eUpd) =>
              update(eUpd)
          }

        case Update.Leaf(l, Change(_, now)) => Vec(TreeTableView.NodeChanged(l, Data.Leaf(now)))
      }

    private val r = new DefaultTreeTableCellRenderer

    def renderer(view: TreeTableView[S, Node, Branch, Data], data: Data, row: Int, column: Int,
                 state: State): Component = {
      val value = data match {
        case Data.Branch  => if (column == 0) "Branch" else ""
        case Data.Leaf(v) => if (column == 0) "Leaf"   else v.toString
      }

      val c = state.tree.fold {
        r.getTreeTableCellRendererComponent(view.treeTable.peer, value, state.selected, state.focused, row, column)
      } { ts =>
        r.getTreeTableCellRendererComponent(view.treeTable.peer, value, state.selected, state.focused,
          row, column, ts.expanded, ts.leaf)
      }
      Component.wrap(c.asInstanceOf[JComponent])
    }

    val col1 = new TreeColumnModel.Column[Data, String]("Foo") {
      def apply(data: Data): String = data match {
        case Data.Branch  => "Branch"
        case Data.Leaf(_) => "Leaf"
      }

      def isEditable(node: Data): Boolean = false

      def update(node: Data, value: String): Unit = ()
    }

    val col2 = new TreeColumnModel.Column[Data, String]("Bar") {
      def apply(data: Data): String = data match {
        case Data.Branch  => ""
        case Data.Leaf(v) => v.toString
      }

      def isEditable(node: Data): Boolean = true

      def update(data: Data, value: String): Unit = ()
//        try {
//        val i = Integer.parseInt(value)
//      } catch {
//        case NonFatal(_) =>
//      }
    }

    val columns: TreeColumnModel[Data] = new TreeColumnModel.Tuple2(col1, col2) {
      def getParent(data: Data): Option[Data] = None
    }

    def data(node: Node)(implicit tx:  S#Tx): Data = node match {
      case b: Branch  => Data.Branch
      case l: Leaf    => Data.Leaf(l.expr.value)
    }
  }

  def newBranch()(implicit tx: S#Tx) = {
    val li    = expr.List.Modifiable[S, Node, Node.Update]
    val tgt   = evt.Targets[S]
    new Branch(tgt, li)
  }

  def newLeaf()(implicit tx: S#Tx) = {
    val ex    = lucre.expr.Int.newVar[S](lucre.expr.Int.newConst((math.random * 100).toInt))
    val tgt   = evt.Targets[S]
    new Leaf(tgt, ex)
  }

  private lazy val (treeH, view) = system.step { implicit tx =>
    val root = newBranch()
    tx.newHandle(root) -> TreeTableView(root, h)
  }

  private def scramble(s: String): String = {
    val sb  = new StringBuilder
    var bag = (0 until s.length).toIndexedSeq
    while (bag.nonEmpty) {
      val i = (math.random * bag.size).toInt
      val j = bag(i)
      bag   = bag.patch(i, Nil, 1)
      sb.append(s.charAt(j))
    }
    sb.result()
  }

  private def add(child: Node)(implicit tx: S#Tx): Unit = {
    val (parent, idx) = view.insertionPoint()
    parent.children.insert(idx, child)
  }

  private def addBranchAction(): Unit = system.step { implicit tx =>
    add(newBranch())
  }

  private def addLeafAction(): Unit = system.step { implicit tx =>
    add(newLeaf())
  }

  private def removeAction(): Unit = system.step { implicit tx =>
    val toRemove = view.selection.flatMap { childView =>
      childView.parentOption.flatMap { p =>
        p.modelData() match {
          case b: Branch => Some(b -> childView.modelData())
          case _ => None
        }
      }
    }
    toRemove.reverse.foreach { case (parent, child) =>
      parent.children.remove(child)
    }
  }

  private def modifyAction(): Unit = system.step { implicit tx =>
    view.selection.foreach { v =>
      v.modelData() match {
        case l: Leaf => l.expr() = lucre.expr.Int.newConst[S]((math.random * 100).toInt)
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
