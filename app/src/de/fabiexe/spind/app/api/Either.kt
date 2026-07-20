package de.fabiexe.spind

/**
 * A type that can hold one of two possible values: a value of type [A] (represented by [Left])
 * or a value of type [B] (represented by [Right]).
 */
sealed class Either<out A, out B> {
    /**
     * Represents a value of type [A]
     *
     * @param value The value of type [A]
     */
    class Left<A>(val value: A) : Either<A, Nothing>()

    /**
     * Represents a value of type [B]
     *
     * @param value The value of type [B]
     */
    class Right<B>(val value: B) : Either<Nothing, B>()
}