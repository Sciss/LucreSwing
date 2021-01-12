/*
 *  ListViewImpl.scala
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

package de.sciss.lucre.swing.impl

import de.sciss.lucre.{ListObj, Txn}
import de.sciss.lucre.swing.ListView.Handler
import de.sciss.lucre.swing.LucreSwing.{deferTx, requireEDT}
import de.sciss.lucre.Log.{swing => log}
import de.sciss.lucre.swing.{ListView, Observation}
import de.sciss.model.impl.ModelImpl
import de.sciss.serial.TFormat
import de.sciss.swingplus

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.stm.Ref
import scala.swing.event.ListSelectionChanged
import scala.swing.{Component, ScrollPane}

object ListViewImpl {
  def empty[T <: Txn[T], Elem, U, Data](handler: Handler[T, Elem, U, Data])
                                       (implicit tx: T,
                                        format: TFormat[T, ListObj[T, Elem]]): ListView[T, Elem, U] = {
    val view = new Impl[T, Elem, U, Data](handler)
    deferTx {
      view.guiInit()
    }
    view
  }

  def apply[T <: Txn[T], Elem, U, Data](list: ListObj[T, Elem], handler: Handler[T, Elem, U, Data])
                                       (implicit tx: T,
                                        format: TFormat[T, ListObj[T, Elem]]): ListView[T, Elem, U] = {
    val view = empty[T, Elem, U, Data](handler)
    view.list_=(Some(list))
    view
  }

  private final class Impl[T <: Txn[T], Elem, U, Data](handler: Handler[T, Elem, U, Data])
                                                      (implicit format: TFormat[T, ListObj[T, Elem]])
    extends ListView[T, Elem, U] with ComponentHolder[Component] with ModelImpl[ListView.Update] {
    impl =>

    private var ggList: swingplus.ListView[Data] = _
    private val mList   = swingplus.ListView.Model.empty[Data]
    private val current = Ref(Option.empty[Observation[T, ListObj[T, Elem]]])

    def view: swingplus.ListView[Data] = ggList

    def list(implicit tx: T): Option[ListObj[T, Elem]] = current.get(tx.peer).map(_.value())

    def list_=(newOption: Option[ListObj[T, Elem]])(implicit tx: T): Unit = {
      disposeList()
      val newObsOpt = newOption.map(Observation(_) { implicit tx => upd =>
        log.debug(s"ListView ${impl.hashCode.toHexString} react")
        upd.changes.foreach {
          case ListObj.Added(  idx, elem)  => val item = handler.data(elem); deferTx(impl.insertItem(idx, item))
          case ListObj.Removed(idx, _   )  => deferTx(impl.removeItemAt(idx))
// ELEM
//          case List.Element(elem, eu )  =>
//            val idx = upd.list.indexOf(elem)
//            if (idx >= 0) {
//              handler.dataUpdate(elem, eu).foreach { item =>
//                deferTx(impl.updateItemAt(idx, item))
//              }
//            }
        }
      })  // IntelliJ highlight bug
      current.set(newObsOpt)(tx.peer)
      val items = newOption.fold(Vec.empty[Data])(ll => ll.iterator.map(handler.data).toIndexedSeq)
      deferTx {
        impl.replaceItems(items)
      }
    }

    private def disposeList()(implicit tx: T): Unit = {
      current.swap(None)(tx.peer).foreach { obs =>
        log.debug(s"disposeList(); obs = $obs")
        obs.dispose()
      }
    }

    //    private def createObserver(ll: List[T, Elem, U])(implicit tx: T): Disposable[T] = {
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

    def dispose()(implicit tx: T): Unit = {
      list_=(None)
      deferTx {
        releaseListeners()
      }
    }
  }
}
