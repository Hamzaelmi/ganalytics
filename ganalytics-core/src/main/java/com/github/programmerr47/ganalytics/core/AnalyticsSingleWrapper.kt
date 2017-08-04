package com.github.programmerr47.ganalytics.core

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

class AnalyticsSingleWrapper(
        private val eventProvider: EventProvider,
        private val globalSettings: GanalyticsSettings = GanalyticsSettings(),
        private val defAnnotations: AnalyticsDefAnnotations = AnalyticsDefAnnotations()) : AnalyticsWrapper {

    @Suppress("unchecked_cast")
    override fun <T : Any> create(clazz: Class<T>): T {
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf<Class<*>>(clazz)) { _, method, args ->
            val convention = getAnnotation(Convention::class, clazz, defAnnotations)?.value ?: globalSettings.namingConvention
            val category = applyCategory(applyConvention(convention, clazz.analyticsName), clazz, defAnnotations)

            val defaultAction = applyAction(method, applyConvention(convention, method.name))
            val action = if (getAnnotation(NoPrefix::class, method, clazz, defAnnotations) != null) {
                defaultAction
            } else {
                applyPrefix(defaultAction, category, method, clazz, defAnnotations)
            }

            val (labelArg, valueArg) = manageLabelValueArgs(method, args)
            val label = labelArg ?: ""
            val value = (valueArg ?: 0).toLong()

            val event = Event(category, action, label, value)
            eventProvider.provide(event)
        } as T
    }

    private val Class<*>.analyticsName get() = if (globalSettings.cutOffAnalyticsClassPrefix)
        simpleName.decapitalize().removePrefix("analytics").capitalize()
    else
        simpleName

    private fun applyConvention(convention: NamingConvention, name: String) = convention
            .withFirstFixingBadCodeStyle()
            .convert(name.decapitalize())

    private fun applyCategory(default: String, vararg elements: AnnotatedElement) =
            applyCategory(getAnnotation(Category::class, *elements), default)

    private fun applyCategory(category: Category?, default: String): String {
        return category?.name?.takeNotEmpty() ?: default
    }

    private fun applyAction(element: AnnotatedElement, default: String): String {
        return applyAction(element.getAnnotation(Action::class.java), default)
    }

    private fun applyAction(action: Action?, default: String): String {
        return action?.name?.takeNotEmpty() ?: default
    }

    private fun applyPrefix(input: String, default: String, vararg elements: AnnotatedElement): String {
        return applyPrefix(input, default, getAnnotation(HasPrefix::class, *elements))
    }

    private fun applyPrefix(input: String, default: String, hasPrefix: HasPrefix?): String {
        return if (hasPrefix == null) input else applyPrefix(input, default, hasPrefix.name, hasPrefix.globalSplitter)
    }

    private val HasPrefix.globalSplitter get() = if (splitter == "") globalSettings.prefixSplitter else splitter

    private fun applyPrefix(input: String, default: String, prefix: String, splitter: String): String {
        return (prefix.getOr(default)) + splitter + input
    }

    private fun String.getOr(default: String) = takeNotEmpty() ?: default

    private fun String.takeNotEmpty() = takeUnless(String::isEmpty)

    private fun <T : Annotation> getAnnotation(clazz: KClass<T>, vararg elements: AnnotatedElement): T? {
        return elements.map { it.getAnnotation(clazz.java) }.firstOrNull { it != null }
    }

    private fun manageLabelValueArgs(method: Method, args: Array<Any>?) = when (args?.size) {
        in arrayOf(0, null) -> Pair(null, null)
        1 -> Pair(convertLabelArg(args[0], method.parameterAnnotations[0]), null)
        2 -> manageTwoArgs(args, method.parameterAnnotations)
        else -> throw IllegalArgumentException("Method ${method.name} have ${method.parameterCount} parameter(s). You can have up to 2 parameters in methods.")
    }

    private fun manageTwoArgs(args: Array<Any>, annotations: Array<Array<Annotation>>): Pair<String, Number> {
        return manageTwoArgs(args[0], annotations[0].label(), args[1], annotations[1].label())
    }

    private fun Array<Annotation>.label() = firstOrNull(Label::class)

    private fun manageTwoArgs(arg1: Any, argA1: Label?, arg2: Any, argA2: Label?): Pair<String, Number> {
        return manageArgAsValue(arg2, argA2, arg1, argA1) {
            manageArgAsValue(arg1, argA1, arg2, argA2) {
                throw IllegalArgumentException("For methods with 2 parameters one of them have to be Number without Label annotation")
            }
        }
    }

    private inline fun manageArgAsValue(vArg: Any, vArgA: Label?, lArg: Any, lArgA: Label?, defaultAction: () -> Pair<String, Number>): Pair<String, Number> {
        return if (vArg is Number && vArgA == null) {
            Pair(convertLabelArg(lArg, lArgA), vArg)
        } else {
            defaultAction()
        }
    }

    private fun convertLabelArg(label: Any, annotations: Array<Annotation>): String {
        return convertLabelArg(label, annotations.firstOrNull(Label::class))
    }

    private fun convertLabelArg(label: Any, annotation: Label?): String {
        return chooseConverter(label, annotation).convert(label)
    }

    private fun chooseConverter(label: Any, annotation: Label?): LabelConverter {
        return annotation?.converter?.init() ?: lookupGlobalConverter(label) ?: SimpleLabelConverter
    }

    private fun lookupGlobalConverter(label: Any): LabelConverter? {
        label.converterClasses().forEach {
            val converter = globalSettings.labelTypeConverters.lookup(it)
            if (converter != null) return converter
        }
        return null
    }

    private fun Any.converterClasses() = if (globalSettings.useTypeConvertersForSubType)
        javaClass.classHierarchy()
    else
        arrayListOf(javaClass)

    private fun Class<in Any>.classHierarchy() = ArrayList<Class<Any>>().also {
        var clazz: Class<in Any>? = this
        do {
            it.add(clazz!!)
            clazz = clazz.superclass
        } while (clazz != null)
    }

    private fun KClass<out LabelConverter>.init() = objectInstance ?: java.newInstance()

    private fun <R : Any> Array<*>.firstOrNull(klass: KClass<R>): R? {
        return filterIsInstance(klass.java).firstOrNull()
    }
}
