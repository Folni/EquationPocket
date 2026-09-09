package hr.algebra.equationpocket

import org.junit.Test
import org.junit.Assert.*
import org.matheclipse.core.eval.ExprEvaluator

/**
 * Unit testovi za provjeru matematičke logike aplikacije EquationPocket.
 * Testiramo integraciju sa Symja CAS engine-om.
 */
class MathLogicUnitTest {

    private val evaluator = ExprEvaluator()

    @Test
    fun testSimpleLinearEquation() {
        // Jednadžba: 2*x = 10
        val expression = "Solve(2*x == 10, x)"
        val result = evaluator.evaluate(expression)
        // Očekivani rezultat u Symja formatu je {{x -> 5}}
        assertTrue(result.toString().contains("5"))
    }

    @Test
    fun testQuadraticEquation() {
        // Jednadžba: x^2 = 9
        val expression = "Solve(x^2 == 9, x)"
        val result = evaluator.evaluate(expression)
        // Rezultat bi trebao sadržavati oba rješenja: 3 i -3
        val resStr = result.toString()
        assertTrue(resStr.contains("3") && resStr.contains("-3"))
    }

    @Test
    fun testDivisionByZero() {
        // Pokušaj dijeljenja s nulom
        val expression = "1 / 0"
        val result = evaluator.evaluate(expression)
        // Symja vraća ComplexInfinity za dijeljenje s nulom
        assertEquals("ComplexInfinity", result.toString())
    }

    @Test
    fun testScientificNotation() {
        // Testiranje znanstvenog zapisa: 10^3 * 2
        val expression = "10^3 * 2"
        val result = evaluator.evaluate(expression)
        assertEquals("2000", result.toString())
    }

    @Test
    fun testDerivatives() {
        // Derivacija od x^3 po x
        val expression = "D(x^3, x)"
        val result = evaluator.evaluate(expression)
        // Očekivani rezultat je 3*x^2
        assertEquals("3*x^2", result.toString())
    }

    @Test
    fun testIntegrals() {
        // Integral od x^2 po x
        val expression = "Integrate(x^2, x)"
        val result = evaluator.evaluate(expression)
        // Očekivani rezultat je x^3 / 3
        assertEquals("x^3/3", result.toString().replace(" ", ""))
    }
}
