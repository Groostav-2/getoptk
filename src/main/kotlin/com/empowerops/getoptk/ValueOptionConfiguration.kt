package com.empowerops.getoptk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class ValueOptionConfiguration<T: Any>(
        source: CLI,
        optionType: KClass<T>,
        private val userConfig: ValueOptionConfiguration<T>.() -> Unit
) : CommandLineOption<T>, OptionCombinator {

    init { RegisteredOptions.optionProperties += source to this }

    var converter: Converter<T> = Converters.getDefaultFor(optionType)

    override lateinit var shortName: String
    override lateinit var longName: String
    override lateinit var description: String

    internal var initialized = false
    internal var _value: T? = null

    override fun toString() = "--$longName <arg>"

    override operator fun getValue(thisRef: CLI, property: KProperty<*>): T{
        require(initialized) { "TODO: nice error message" }
        return _value!! //uhh, how do I make this nullable iff user specified T as "String?" or some such?
    }

    override fun finalizeInit(hostingProperty: KProperty<*>) {
        description = Inferred.generateInferredDescription(hostingProperty)
        longName = Inferred.generateInferredLongName(hostingProperty)
        shortName = Inferred.generateInferredShortName(hostingProperty)

        userConfig()
    }

    override fun reduce(tokens: List<Token>): List<Token> = with(Marker(tokens)){

        if( ! (nextIs<OptionPreambleToken>()
                && nextIs<OptionName> { it.text in names() }
                && nextIs<SuperTokenSeparator>())) {
            return tokens
        }

        val argText = (next() as? Argument)?.text ?: return tokens

        _value = converter.convert(argText)
        initialized = true

        expect<SuperTokenSeparator>()

        return rest()
    }
}

