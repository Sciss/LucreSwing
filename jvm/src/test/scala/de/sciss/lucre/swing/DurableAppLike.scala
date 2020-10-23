package de.sciss.lucre.swing

import de.sciss.file.File
import de.sciss.lucre.store.BerkeleyDB
import de.sciss.lucre.{Cursor, Durable}

trait DurableAppLike extends AppLike {
  type S = Durable
  type T = Durable.Txn
  implicit val system: S with Cursor[T] = Durable(BerkeleyDB.factory(File.createTemp(directory = true)))
}
