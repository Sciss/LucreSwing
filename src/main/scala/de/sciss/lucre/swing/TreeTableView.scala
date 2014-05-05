/*
 *  TreeTableView.scala
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

import de.sciss.lucre.{event => evt}
import evt.Sys
import de.sciss.treetable.{TreeTable, TreeTableCellRenderer, TreeColumnModel}
import scala.swing.Component
import de.sciss.model.Model
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Disposable
import de.sciss.serial.Serializer
import impl.{TreeTableViewImpl => Impl}
import scala.collection.immutable.{IndexedSeq => Vec}

object TreeTableView {
  sealed trait ModelUpdate[+Node, +Data]
  final case class NodeAdded  [Node](index: Int, child: Node) extends ModelUpdate[Node   , Nothing]
  final case class NodeRemoved[Node](index: Int, child: Node) extends ModelUpdate[Node   , Nothing]
  final case class NodeChanged[Node, Data](child: Node, data: Data) extends ModelUpdate[Node, Data]

  final case class Nested[Node, Data](index: Int, child: Node, u: ModelUpdate[Node, Data])
    extends ModelUpdate[Node, Data]

  trait Handler[S <: Sys[S], Node, Branch, U, Data] {
    def branchOption(node: Node): Option[Branch]

    /** The `Node` type must have an identifier. It is used to map between nodes
      * and their views. This method queries the identifier of a given node.
      */
    def nodeID(node: Node): S#ID

    /** Queries the opaque rendering data for a given node. The data can be used by the renderer. */
    def data(node: Node)(implicit tx: S#Tx): Data

    /** Queries the children of a node. If the node is not a branch, the method should return `None`,
      * otherwise it should return an iterator over the child nodes.
      */
    def children(branch: Branch)(implicit tx: S#Tx): de.sciss.lucre.data.Iterator[S#Tx, Node]

    /** Note: this model is wrapped. The `getParent` method is never used and can safely be implemented
      * by returning `None` always.
      */
    def columns: TreeColumnModel[Data] // NodeView[S, Node, Data]]

    def renderer(view: TreeTableView[S, Node, Branch, Data], data: Data, row: Int, column: Int,
                 state: TreeTableCellRenderer.State): Component

    /** Notifies the handler that a node has seen an update. The handler then casts that opaque update type
      * to one of the resolved `ModelUpdate` types. If the update is irrelevant for the view, the method
      * should return `None`.
      *
      * @param  node    the  node which has been updated
      * @param  update  the type of update
      * @param  data    the previous view data
      */
    def update(/* node: Node, */ update: U /* , data: Data */)(implicit tx: S#Tx): Vec[ModelUpdate[Node, Data]]
  }

  def apply[S <: Sys[S], Node, Branch <: evt.Publisher[S, U], U, Data](root: Branch,
                                                                       handler: Handler[S, Node, Branch, U, Data])(
      implicit tx: S#Tx, nodeSerializer: Serializer[S#Tx, S#Acc, Node],
      branchSerializer: Serializer[S#Tx, S#Acc, Branch]): TreeTableView[S, Node, Branch, Data] =
    Impl(root, handler)

  sealed trait Update
  case object SelectionChanged extends Update

  /** The node view encapsulates the rendering data. */
  trait NodeView[S <: Sys[S], Node, Data] {
    def isLeaf: Boolean
    // def isExpanded: Boolean

    def renderData: Data
    // def modelData()(implicit tx: S#Tx): TreeLike.Node[T#Branch, T#Leaf]
    def modelData: stm.Source[S#Tx, Node]
    def parentOption: Option[NodeView[S, Node, Data]]
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
  type NodeView <: TreeTableView.NodeView[S, Node, Data]

  def component: Component
  def treeTable: TreeTable[_, _]

  def selection: List[NodeView]

  def markInsertion()(implicit tx: S#Tx): Unit

  def insertionPoint()(implicit tx: S#Tx): (Branch, Int)

  // /** Maps from view to underlying model data. */
  // def data(view: Node)(implicit tx: S#Tx): TreeLike.Node[T#Branch, T#Leaf]
}
