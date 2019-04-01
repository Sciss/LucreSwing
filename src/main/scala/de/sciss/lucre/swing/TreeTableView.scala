/*
 *  TreeTableView.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import javax.swing.CellEditor

import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Disposable, Identifiable, Sys}
import de.sciss.lucre.swing.impl.{TreeTableViewImpl => Impl}
import de.sciss.model.Model
import de.sciss.serial.Serializer
import de.sciss.treetable.{TreeTable, TreeTableCellRenderer}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.Component

object TreeTableView {
  sealed trait ModelUpdate[/*+ */Node, +Branch]
  final case class NodeAdded  [Node, Branch](parent: Branch, index: Int, child: Node) extends ModelUpdate[Node, Branch]
  final case class NodeRemoved[Node, Branch](parent: Branch, index: Int, child: Node) extends ModelUpdate[Node, Branch]
  final case class NodeChanged[Node](node: Node) extends ModelUpdate[Node, Nothing]

  //  final case class Nested[Node, Data](index: Int, child: Node, u: ModelUpdate[Node, Data])
  //    extends ModelUpdate[Node, Data]

  trait Handler[S <: Sys[S], Node, Branch <: Node, Data] {
    def branchOption(node: Node): Option[Branch]

    //    /** The `Node` type must have an identifier. It is used to map between nodes
    //      * and their views. This method queries the identifier of a given node.
    //      */
    //    def nodeId(node: Node): S#Id

    /** Queries the opaque rendering data for a given node. The data can be used by the renderer. */
    def data(node: Node)(implicit tx: S#Tx): Data

    /** Queries the children of a node. If the node is not a branch, the method should return `None`,
      * otherwise it should return an iterator over the child nodes.
      */
    def children(branch: Branch)(implicit tx: S#Tx): Iterator[Node]

    //    /** Note: this model is wrapped. The `getParent` method is never used and can safely be implemented
    //      * by returning `None` always.
    //      */
    //    def columns: TreeColumnModel[Data] // NodeView[S, Node, Data]]

    def renderer(treeTable: TreeTableView[S, Node, Branch, Data], node: NodeView[S, Node, Branch, Data],
                 row: Int, column: Int, state: TreeTableCellRenderer.State): Component

    // def isEditable(data: Data, row: Int, column: Int): Boolean

    def editor(treeTable: TreeTableView[S, Node, Branch, Data], node: NodeView[S, Node, Branch, Data],
               row: Int, column: Int, selected: Boolean): (Component, CellEditor)
    
    def isEditable(data: Data, column: Int): Boolean

    def columnNames: Vec[String]

    def observe(n: Node, dispatch: S#Tx => ModelUpdate[Node, Branch] => Unit)(implicit tx: S#Tx): Disposable[S#Tx]

//    /** Notifies the handler that a node has seen an update. The handler then casts that opaque update type
//      * to one of the resolved `ModelUpdate` types. If the update is irrelevant for the view, the method
//      * should return `None`.
//      *
//      * @param  update  the type of update
//      */
//    def mapUpdate(/* node: Node, */ update: U /* , data: Data */)(implicit tx: S#Tx): Vec[ModelUpdate[Node, Branch]]
  }

  def apply[S <: Sys[S], Node <: Identifiable[S#Id], Branch <: Node, Data](
        root: Branch, handler: Handler[S, Node, Branch, Data])(
        implicit tx: S#Tx, nodeSerializer: Serializer[S#Tx, S#Acc, Node],
      branchSerializer: Serializer[S#Tx, S#Acc, Branch]): TreeTableView[S, Node, Branch, Data] =
    Impl(root, handler)

  sealed trait Update
  case object SelectionChanged extends Update

  /** The node view encapsulates the rendering data. */
  trait NodeView[S <: Sys[S], Node, Branch, Data] {
    def isLeaf: Boolean
    // def isExpanded: Boolean

    def renderData: Data
    // def modelData()(implicit tx: S#Tx): TreeLike.Node[T#Branch, T#Leaf]
    def modelData: stm.Source[S#Tx, Node]
    def parentView: Option[NodeView[S, Node, Branch, Data]]
    def parent(implicit tx: S#Tx): Branch
  }
}

/** A view for tree like data that is presented as a tree table.
  *
  * @tparam S     the system in which the tree is represented
  * @tparam Node  the opaque node type of the tree, encompassing both branches and leaves
  * @tparam Data  the opaque data type which is a non-transactional view structure used for rendering the nodes.
  */
trait TreeTableView[S <: Sys[S], Node, Branch, Data]
  extends Disposable[S#Tx] with Model[TreeTableView.Update] {

  /** Opaque view type corresponding with a node in the model. */
  type NodeView <: TreeTableView.NodeView[S, Node, Branch, Data]

  def component: Component

  def treeTable: TreeTable[_, _]

  /** The drop location in a drag-and-drop operation. This returns `None` if no drop action is currently
    * being performed. Please note that the root element is removed. It is thus possible that
    * the path is empty!
    */
  def dropLocation: Option[TreeTable.DropLocation[NodeView]]

  def root: stm.Source[S#Tx, Branch]

  def nodeView(node: Node)(implicit tx: S#Tx): Option[NodeView]

  def selection: List[NodeView]

  def markInsertion()(implicit tx: S#Tx): Unit

  def insertionPoint(implicit tx: S#Tx): (Branch, Int)

  // /** Maps from view to underlying model data. */
  // def data(view: Node)(implicit tx: S#Tx): TreeLike.Node[T#Branch, T#Leaf]
}
