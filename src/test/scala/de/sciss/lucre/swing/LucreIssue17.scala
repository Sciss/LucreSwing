// we would need Obj.Bridge[Option[A]]

//package de.sciss.lucre.swing
//
//import de.sciss.lucre.expr.graph.Obj
//
//trait LucreIssue17 {
//  import graph._
//  import de.sciss.lucre.expr.ExImport._
//
//  implicit def foo: Obj.Bridge[Option[Int]]
//
//  val ggChunkSize = ComboBox[Int](
//    List(2, 4, 8, 16, 32, 64)
//  )
////  ggChunkSize.valueOption <--> "run:chunk-size".attr[Int]
//  ggChunkSize.valueOption <--> "run:chunk-size".attr[Option[Int]](None)
//}
