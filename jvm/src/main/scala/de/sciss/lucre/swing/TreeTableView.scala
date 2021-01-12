/*
 *  TreeTableView.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2021 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.lucre.{Disposable, Identified, Source, Txn}
import de.sciss.lucre.swing.impl.{TreeTableViewImpl => Impl}
import de.sciss.model.Model
import de.sciss.serial.TFormat
import de.sciss.treetable.{TreeTable, TreeTableCellRenderer}
import javax.swing.CellEditor

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.Component

object TreeTableView {
  sealed trait ModelUpdate[/*+ */Node, +Branch]
  final case class NodeAdded  [Node, Branch](parent: Branch, index: Int, child: Node) extends ModelUpdate[Node, Branch]
  final case class NodeRemoved[Node, Branch](parent: Branch, index: Int, child: Node) extends ModelUpdate[Node, Branch]
  final case class NodeChanged[Node](node: Node) extends ModelUpdate[Node, Nothing]

  //  final case class Nested[Node, Data](index: Int, child: Node, u: ModelUpdate[Node, Data])
  //    extends ModelUpdate[Node, Data]

  trait Handler[T <: Txn[T], Node, Branch <: Node, Data] {
    def branchOption(node: Node): Option[Branch]

    //    /** The `Node` type must have an identifier. It is used to map between nodes
    //      * and their views. This method queries the identifier of a given node.
    //      */
    //    def nodeId(node: Node): Ident[T]

    /** Queries the opaque rendering data for a given node. The data can be used by the renderer. */
    def data(node: Node)(implicit tx: T): Data

    /** Queries the children of a node. If the node is not a branch, the method should return `None`,
      * otherwise it should return an iterator over the child nodes.
      */
    def children(branch: Branch)(implicit tx: T): Iterator[Node]

    //    /** Note: this model is wrapped. The `getParent` method is never used and can safely be implemented
    //      * by returning `None` always.
    //      */
    //    def columns: TreeColumnModel[Data] // NodeView[T, Node, Data]]

    def renderer(treeTable: TreeTableView[T, Node, Branch, Data], node: NodeView[T, Node, Branch, Data],
                 row: Int, column: Int, state: TreeTableCellRenderer.State): Component

    // def isEditable(data: Data, row: Int, column: Int): Boolean

    def editor(treeTable: TreeTableView[T, Node, Branch, Data], node: NodeView[T, Node, Branch, Data],
               row: Int, column: Int, selected: Boolean): (Component, CellEditor)
    
    def isEditable(data: Data, column: Int): Boolean

    def columnNames: Vec[String]

    def observe(n: Node, dispatch: T => ModelUpdate[Node, Branch] => Unit)(implicit tx: T): Disposable[T]

//    /** Notifies the handler that a node has seen an update. The handler then casts that opaque update type
//      * to one of the resolved `ModelUpdate` types. If the update is irrelevant for the view, the method
//      * should return `None`.
//      *
//      * @param  update  the type of update
//      */
//    def mapUpdate(/* node: Node, */ update: U /* , data: Data */)(implicit tx: T): Vec[ModelUpdate[Node, Branch]]
  }

  def apply[T <: Txn[T], Node <: Identified[T],
    Branch <: Node, Data](root: Branch, handler: Handler[T, Node, Branch, Data])
                         (implicit tx: T, nodeFormat: TFormat[T, Node],
                          branchFormat: TFormat[T, Branch]): TreeTableView[T, Node, Branch, Data] =
    Impl(root, handler)

  sealed trait Update
  case object SelectionChanged extends Update

  /** The node view encapsulates the rendering data. */
  trait NodeView[T <: Txn[T], Node, Branch, Data] {
    def isLeaf: Boolean
    // def isExpanded: Boolean

    def renderData: Data
    // def modelData()(implicit tx: T): TreeLike.Node[T#Branch, T#Leaf]
    def modelData: Source[T, Node]
    def parentView: Option[NodeView[T, Node, Branch, Data]]
    def parent(implicit tx: T): Branch
  }
}

/** A view for tree like data that is presented as a tree table.
  *
  * @tparam Node  the opaque node type of the tree, encompassing both branches and leaves
  * @tparam Data  the opaque data type which is a non-transactional view structure used for rendering the nodes.
  */
trait TreeTableView[T <: Txn[T], Node, Branch, Data]
  extends Disposable[T] with Model[TreeTableView.Update] {

  /** Opaque view type corresponding with a node in the model. */
  type NodeView <: TreeTableView.NodeView[T, Node, Branch, Data]

  def component: Component

  def treeTable: TreeTable[_, _]

  /** The drop location in a drag-and-drop operation. This returns `None` if no drop action is currently
    * being performed. Please note that the root element is removed. It is thus possible that
    * the path is empty!
    */
  def dropLocation: Option[TreeTable.DropLocation[NodeView]]

  def root: Source[T, Branch]

  def nodeView(node: Node)(implicit tx: T): Option[NodeView]

  def selection: List[NodeView]

  def markInsertion()(implicit tx: T): Unit

  def insertionPoint(implicit tx: T): (Branch, Int)

  // /** Maps from view to underlying model data. */
  // def data(view: Node)(implicit tx: T): TreeLike.Node[T#Branch, T#Leaf]
}
