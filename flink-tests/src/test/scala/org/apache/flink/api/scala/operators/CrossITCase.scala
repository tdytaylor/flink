/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.api.scala.operators

import org.apache.flink.api.common.functions.RichCrossFunction
import org.apache.flink.api.scala.util.CollectionDataSets
import org.apache.flink.api.scala.util.CollectionDataSets.CustomType
import org.apache.flink.configuration.Configuration
import org.apache.flink.test.util.JavaProgramTestBase
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConverters._
import scala.collection.mutable

import org.apache.flink.api.scala._


object CrossProgs {
  var NUM_PROGRAMS: Int = 9

  def runProgram(progId: Int, resultPath: String): String = {
    progId match {
      case 1 =>
        /*
         * check correctness of cross on two tuple inputs
         */
        val env = ExecutionEnvironment.getExecutionEnvironment
        val ds = CollectionDataSets.getSmall5TupleDataSet(env)
        val ds2 = CollectionDataSets.getSmall5TupleDataSet(env)
        val crossDs = ds.cross(ds2) { (l, r) => (l._3 + r._3, l._4 + r._4) }
        crossDs.writeAsCsv(resultPath)
        env.execute()

        "0,HalloHallo\n" + "1,HalloHallo Welt\n" + "2,HalloHallo Welt wie\n" + "1," +
          "Hallo WeltHallo\n" + "2,Hallo WeltHallo Welt\n" + "3,Hallo WeltHallo Welt wie\n" + "2," +
          "Hallo Welt wieHallo\n" + "3,Hallo Welt wieHallo Welt\n" + "4," +
          "Hallo Welt wieHallo Welt wie\n"

      case 2 =>
        /*
         * check correctness of cross if UDF returns left input object
         */
        val env = ExecutionEnvironment.getExecutionEnvironment
        val ds = CollectionDataSets.getSmall3TupleDataSet(env)
        val ds2 = CollectionDataSets.getSmall5TupleDataSet(env)
        val crossDs = ds.cross(ds2) { (l, r ) => l }
        crossDs.writeAsCsv(resultPath)
        env.execute()

        "1,1,Hi\n" + "1,1,Hi\n" + "1,1,Hi\n" + "2,2,Hello\n" + "2,2,Hello\n" + "2,2," +
          "Hello\n" + "3,2,Hello world\n" + "3,2,Hello world\n" + "3,2,Hello world\n"

      case 3 =>
        /*
         * check correctness of cross if UDF returns right input object
         */
        val env = ExecutionEnvironment.getExecutionEnvironment
        val ds = CollectionDataSets.getSmall3TupleDataSet(env)
        val ds2 = CollectionDataSets.getSmall5TupleDataSet(env)
        val crossDs = ds.cross(ds2) { (l, r) => r }
        crossDs.writeAsCsv(resultPath)
        env.execute()

        "1,1,0,Hallo,1\n" + "1,1,0,Hallo,1\n" + "1,1,0,Hallo,1\n" + "2,2,1,Hallo Welt," +
          "2\n" + "2,2,1,Hallo Welt,2\n" + "2,2,1,Hallo Welt,2\n" + "2,3,2,Hallo Welt wie," +
          "1\n" + "2,3,2,Hallo Welt wie,1\n" + "2,3,2,Hallo Welt wie,1\n"

      case 4 =>
        /*
         * check correctness of cross with broadcast set
         */
        val env = ExecutionEnvironment.getExecutionEnvironment
        val intDs = CollectionDataSets.getIntDataSet(env)
        val ds = CollectionDataSets.getSmall5TupleDataSet(env)
        val ds2 = CollectionDataSets.getSmall5TupleDataSet(env)
        val crossDs = ds.cross(ds2).apply (
          new RichCrossFunction[
          (Int, Long, Int, String, Long),
          (Int, Long, Int, String, Long),
          (Int, Int, Int)] {
          private var broadcast = 41

          override def open(config: Configuration) {
            val ints = this.getRuntimeContext.getBroadcastVariable[Int]("ints").asScala
            broadcast = ints.sum
          }

          override def cross(
              first: (Int, Long, Int, String, Long),
              second: (Int, Long, Int, String, Long)): (Int, Int, Int) = {
            (first._1 + second._1, first._3.toInt * second._3.toInt, broadcast)
          }

        })withBroadcastSet(intDs, "ints")
        crossDs.writeAsCsv(resultPath)
        env.execute()
        "2,0,55\n" + "3,0,55\n" + "3,0,55\n" + "3,0,55\n" + "4,1,55\n" + "4,2,55\n" + "3," +
          "0,55\n" + "4,2,55\n" + "4,4,55\n"

      case 5 =>
        /*
         * check correctness of crossWithHuge (only correctness of result -> should be the same
         * as with normal cross)
         */
        val env = ExecutionEnvironment.getExecutionEnvironment
        val ds = CollectionDataSets.getSmall5TupleDataSet(env)
        val ds2 = CollectionDataSets.getSmall5TupleDataSet(env)
        val crossDs = ds.crossWithHuge(ds2) { (l, r) => (l._3 + r._3, l._4 + r._4)}
        crossDs.writeAsCsv(resultPath)
        env.execute()
        "0,HalloHallo\n" + "1,HalloHallo Welt\n" + "2,HalloHallo Welt wie\n" + "1," +
          "Hallo WeltHallo\n" + "2,Hallo WeltHallo Welt\n" + "3,Hallo WeltHallo Welt wie\n" + "2," +
          "Hallo Welt wieHallo\n" + "3,Hallo Welt wieHallo Welt\n" + "4," +
          "Hallo Welt wieHallo Welt wie\n"

      case 6 =>
        /*
         * check correctness of crossWithTiny (only correctness of result -> should be the same
         * as with normal cross)
         */
        val env = ExecutionEnvironment.getExecutionEnvironment
        val ds = CollectionDataSets
          .getSmall5TupleDataSet(env)
        val ds2 = CollectionDataSets
          .getSmall5TupleDataSet(env)
        val crossDs = ds.crossWithTiny(ds2) { (l, r) => (l._3 + r._3, l._4 + r._4)}
        crossDs.writeAsCsv(resultPath)
        env.execute()
        "0,HalloHallo\n" + "1,HalloHallo Welt\n" + "2,HalloHallo Welt wie\n" + "1," +
          "Hallo WeltHallo\n" + "2,Hallo WeltHallo Welt\n" + "3,Hallo WeltHallo Welt wie\n" + "2," +
          "Hallo Welt wieHallo\n" + "3,Hallo Welt wieHallo Welt\n" + "4," +
          "Hallo Welt wieHallo Welt wie\n"

      case 7 => // 9 in Java CrossITCase
        /*
         * check correctness of default cross
         */
        val env = ExecutionEnvironment.getExecutionEnvironment
        val ds = CollectionDataSets.getSmall3TupleDataSet(env)
        val ds2 = CollectionDataSets.getSmall5TupleDataSet(env)
        val crossDs = ds.cross(ds2)
        crossDs.writeAsCsv(resultPath)
        env.execute()
        "(1,1,Hi),(2,2,1,Hallo Welt,2)\n" + "(1,1,Hi),(1,1,0,Hallo,1)\n" + "(1,1,Hi),(2,3," +
          "2,Hallo Welt wie,1)\n" + "(2,2,Hello),(2,2,1,Hallo Welt,2)\n" + "(2,2,Hello),(1,1,0," +
          "Hallo,1)\n" + "(2,2,Hello),(2,3,2,Hallo Welt wie,1)\n" + "(3,2,Hello world),(2,2,1," +
          "Hallo Welt,2)\n" + "(3,2,Hello world),(1,1,0,Hallo,1)\n" + "(3,2,Hello world),(2,3,2," +
          "Hallo Welt wie,1)\n"

      case 8 => // 10 in Java CrossITCase
        /*
         * check correctness of cross on two custom type inputs
         */
        val env = ExecutionEnvironment.getExecutionEnvironment
        val ds = CollectionDataSets.getSmallCustomTypeDataSet(env)
        val ds2 = CollectionDataSets.getSmallCustomTypeDataSet(env)
        val crossDs = ds.cross(ds2) {
          (l, r) => new CustomType(l.myInt * r.myInt, l.myLong + r.myLong, l.myString + r.myString)
        }
        crossDs.writeAsText(resultPath)
        env.execute()
        "1,0,HiHi\n" + "2,1,HiHello\n" + "2,2,HiHello world\n" + "2,1,HelloHi\n" + "4,2," +
          "HelloHello\n" + "4,3,HelloHello world\n" + "2,2,Hello worldHi\n" + "4,3," +
          "Hello worldHello\n" + "4,4,Hello worldHello world"

      case 9 => // 11 in Java CrossITCase
        /*
         * check correctness of cross a tuple input and a custom type input
         */
        val env = ExecutionEnvironment.getExecutionEnvironment
        val ds = CollectionDataSets.getSmall5TupleDataSet(env)
        val ds2 = CollectionDataSets.getSmallCustomTypeDataSet(env)
        val crossDs = ds.cross(ds2) {
          (l, r) => (l._1 + r.myInt, l._3 * r.myLong, l._4 + r.myString)
        }
        crossDs.writeAsCsv(resultPath)
        env.execute()
        "2,0,HalloHi\n" + "3,0,HalloHello\n" + "3,0,HalloHello world\n" + "3,0," +
          "Hallo WeltHi\n" + "4,1,Hallo WeltHello\n" + "4,2,Hallo WeltHello world\n" + "3,0," +
          "Hallo Welt wieHi\n" + "4,2,Hallo Welt wieHello\n" + "4,4,Hallo Welt wieHello world\n"

      case _ =>
        throw new IllegalArgumentException("Invalid program id")
    }
  }
}


@RunWith(classOf[Parameterized])
class CrossITCase(config: Configuration) extends JavaProgramTestBase(config) {

  private var curProgId: Int = config.getInteger("ProgramId", -1)
  private var resultPath: String = null
  private var expectedResult: String = null

  protected override def preSubmit(): Unit = {
    resultPath = getTempDirPath("result")
  }

  protected def testProgram(): Unit = {
    expectedResult = CrossProgs.runProgram(curProgId, resultPath)
  }

  protected override def postSubmit(): Unit = {
    compareResultsByLinesInMemory(expectedResult, resultPath)
  }
}

object CrossITCase {
  @Parameters
  def getConfigurations: java.util.Collection[Array[AnyRef]] = {
    val configs = mutable.MutableList[Array[AnyRef]]()
    for (i <- 1 to CrossProgs.NUM_PROGRAMS) {
      val config = new Configuration()
      config.setInteger("ProgramId", i)
      configs += Array(config)
    }

    configs.asJavaCollection
  }
}

