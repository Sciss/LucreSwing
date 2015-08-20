/*
 *  ListViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre
package swing
package impl

import de.sciss.lucre.stm.Sys
import de.sciss.lucre.expr.List
import de.sciss.lucre.stm.Cursor
import de.sciss.lucre.swing.ListView.Handler
import de.sciss.model.impl.ModelImpl
import de.sciss.serial.Serializer
import de.sciss.swingplus

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.stm.Ref
import scala.swing.event.ListSelectionChanged
import scala.swing.{Component, ScrollPane}

object ListViewImpl {
  def empty[S <: Sys[S], Elem, U, Data](handler: Handler[S, Elem, U, Data])
                                       (implicit tx: S#Tx, cursor: Cursor[S],
                                        serializer: Serializer[S#Tx, S#Acc, List[S, Elem]]): ListView[S, Elem, U] = {
    val view = new Impl[S, Elem, U, Data](handler)
    deferTx {
      view.guiInit()
    }
    view
  }

  def apply[S <: Sys[S], Elem, U, Data](list: List[S, Elem], handler: Handler[S, Elem, U, Data])
                                       (implicit tx: S#Tx, cursor: Cursor[S],
                                        serializer: Serializer[S#Tx, S#Acc, List[S, Elem]]): ListView[S, Elem, U] = {
    val view = empty[S, Elem, U, Data](handler)
    view.list_=(Some(list))
    view
  }

  private final class Impl[S <: Sys[S], Elem, U, Data](handler: Handler[S, Elem, U, Data])
                                                      (implicit cursor: Cursor[S],
                                                       listSer: Serializer[S#Tx, S#Acc, List[S, Elem]])
    extends ListView[S, Elem, U] with ComponentHolder[Component] with ModelImpl[ListView.Update] {
    impl =>

    private var ggList: swingplus.ListView[Data] = _
    private val mList   = swingplus.ListView.Model.empty[Data]
    private val current = Ref(Option.empty[Observation[S, List[S, Elem]]])

    def view = ggList

    def list(implicit tx: S#Tx): Option[List[S, Elem]] = current.get(tx.peer).map(_.value())

    def list_=(newOption: Option[List[S, Elem]])(implicit tx: S#Tx): Unit = {
      disposeList()
      val newObsOpt = newOption.map(Observation(_) { implicit tx => upd =>
        log(s"ListView ${impl.hashCode.toHexString} react")
        upd.changes.foreach {
          case List.Added(  idx, elem)  => val item = handler.data(elem); deferTx(impl.insertItem(idx, item))
          case List.Removed(idx, elem)  => deferTx(impl.removeItemAt(idx))
          case List.Element(elem, eu )  =>
            val idx = upd.list.indexOf(elem)
            if (idx >= 0) {
              handler.dataUpdate(elem, eu).foreach { item =>
                deferTx(impl.updateItemAt(idx, item))
              }
            }
        }
      })
      current.set(newObsOpt)(tx.peer)
      val items = newOption.fold(Vec.empty[Data])(ll => ll.iterator.map(handler.data).toIndexedSeq)
      deferTx {
        impl.replaceItems(items)
      }
    }

    private def disposeList()(implicit tx: S#Tx): Unit = {
      current.swap(None)(tx.peer).foreach { obs =>
        log(s"disposeList(); obs = $obs")
        obs.dispose()
      }
    }

    //    private def createObserver(ll: List[S, Elem, U])(implicit tx: S#Tx): Disposable[S#Tx] = {
    //      val items = ll.iterator.map(handler.data).toIndexedSeq
    //      deferTx {
    //        view.addAll(items)
    //      }
    //      ll.changed.react { implicit tx => upd => upd.changes.foreach {
    //        case List.Added(  idx, elem)  => val item = handler.data(elem); deferTx(view.add(idx, item))
    //        case List.Removed(idx, elem)  => deferTx(view.remove(idx))
    //        case List.Element(elem, eu )  =>
    //          val idx = upd.list.indexOf(elem)
    //          if (idx >= 0) {
    //            handler.dataUpdate(elem, eu).foreach { item =>
    //              deferTx(view.update(idx, item))
    //            }
    //          }
    //      }
    //      }
    //    }

    private def notifyViewObservers(current: Vec[Int]): Unit = {
      val evt = ListView.SelectionChanged(current)
      dispatch(evt)
    }

    def guiSelection: Vec[Int] = {
      requireEDT()
      ggList.selection.indices.toIndexedSeq
    }

    def guiInit(): Unit = {
      requireEDT()
      //         val rend = new DefaultListCellRenderer {
      //            override def getListCellRendererComponent( c: JList, elem: Any, idx: Int, selected: Boolean, focused: Boolean ) : awt.Component = {
      //               super.getListCellRendererComponent( c, showFun( elem.asInstanceOf[ Elem ]), idx, selected, focused )
      //            }
      //         }
      ggList = new swingplus.ListView[Data](mList) {
        this.dragEnabled = true
        listenTo(selection)
        reactions += {
          case l: ListSelectionChanged[_] => notifyViewObservers(l.range)
        }
      }

      component = new ScrollPane(ggList)
    }

    // def clearItems(): Unit = mList.clear()

    def replaceItems(items: Vec[Data]): Unit = {
      mList.clear()
      mList.insertAll(0, items)
    }

    def insertItem(idx: Int, item: Data): Unit =
      mList.insert(idx, item)

    def removeItemAt(idx: Int): Unit =
      mList.remove(idx)

    def updateItemAt(idx: Int, newItem: Data): Unit = {
      mList.update(idx, newItem)
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      list_=(None)
      deferTx {
        releaseListeners()
      }
    }
  }
}
