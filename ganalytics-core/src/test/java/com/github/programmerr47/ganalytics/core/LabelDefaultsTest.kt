package com.github.programmerr47.ganalytics.core

import org.junit.Test
import java.math.BigDecimal

class LabelDefaultsTest : SingleWrapperTest() {
    @Test
    fun checkLabelInSingleParameterMethods() {
        val dummyClass = DummyClass(101, "Holy world!")
        run(SingleParameterMethodInterface::class) {
            assertEquals(Event("singleparametermethodinterface", "intmethod", "101")) { intMethod(101) }
            assertEquals(Event("singleparametermethodinterface", "strmethod", "Holy world!")) { strMethod("Holy world!") }
            assertEquals(Event("singleparametermethodinterface", "dummyclassmethod", dummyClass.toString())) { dummyClassMethod(dummyClass) }
            assertEquals(Event("singleparametermethodinterface", "dummydataclassmethod", "DummyDataClass(id=101, name=Holy world!)")) { dummyDataClassMethod(DummyDataClass(101, "Holy world!")) }
            assertEquals(Event("singleparametermethodinterface", "dummyenumclassmethod", "TWO")) { dummyEnumClassMethod(DummyEnum.TWO) }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwErrorInMoreThanTwoParameterMethod() {
        run(MoreTwoParameterMethodInterface::class) {
            assertEquals(Event("moretwoparametermethodinterface", "method1")) { method1() }
            method2("1", 2, 3L)
        }
    }

    @Test
    fun checkLabelAndValueInTwoParamMethod() {
        run(TwoParameterMethodStringAndNumberInterface::class) {
            assertEquals(Event("interface", "intstrmethod", "2", 1)) { intStrMethod(1, "2") }
            assertEquals(Event("interface", "strintmethod", "1", 2)) { strIntMethod("1", 2) }
            assertEquals(Event("interface", "strlongmethod", "1", 2)) { strLongMethod("1", 2) }
            assertEquals(Event("interface", "strnumbermethod", "1", 32)) { strNumberMethod("1", BigDecimal(32.3)) }
            assertEquals(Event("interface", "strnumbermethod", "1", 33)) { strNumberMethod("1", 33.9f) }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun checkTwoNoNumberParametersMethod() {
        run(TwoParameterMethodNoNumberInterface::class) {
            assertEquals(Event("we will not", "reach", "this")) { noNumberMethod(DummyEnum.THREE, "I Promise!") }
        }
    }

    @Test
    fun checkTwoParametersOneNumberMethod() {
        run(TwoParameterMethodOneNumberInterface::class) {
            assertEquals(Event("interface", "default", "def", 3)) { default("def", 3) }
            assertEquals(Event("interface", "reversed", "def", 3)) { reversed(3, "def") }
            assertEquals(Event("interface", "custom", "DummyDataClass(id=3, name=def)", 3)) { custom(DummyDataClass(3, "def"), 3) }
        }
    }

    @Test
    fun checkCheckCorrectChoosingBetweenTwoNumberParametersWithoutLabelAnnotation() {
        run(TwoParameterBothNumberInterface::class) {
            assertEquals(Event("interface", "method", "1", 2)) { method(1, 2) }
        }
    }

    interface SingleParameterMethodInterface {
        fun intMethod(param: Int)
        fun strMethod(param: String)
        fun dummyClassMethod(param: DummyClass)
        fun dummyDataClassMethod(param: DummyDataClass)
        fun dummyEnumClassMethod(param: DummyEnum)
    }

    interface MoreTwoParameterMethodInterface {
        fun method1()
        fun method2(param1: String, param2: Int, param3: Long)
    }

    @Category("interface")
    interface TwoParameterMethodStringAndNumberInterface {
        fun intStrMethod(param1: Int, param2: String)
        fun strIntMethod(param1: String, param2: Int)
        fun strLongMethod(param1: String, param2: Long)
        fun strNumberMethod(param1: String, param2: Number)
    }

    @Category("interface")
    interface TwoParameterMethodNoNumberInterface {
        fun noNumberMethod(param1: DummyEnum, param2: String)
    }

    @Category("interface")
    interface TwoParameterMethodOneNumberInterface {
        fun default(param1: String, param2: Number)
        fun reversed(param1: Number, param2: String)
        fun custom(param1: DummyDataClass, param2: Number)
    }

    @Category("interface")
    interface TwoParameterBothNumberInterface {
        fun method(param1: Number, param2: Number)
    }
}