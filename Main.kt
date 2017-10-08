import java.io.File

val kwToLex = hashMapOf(
        Pair("def", "#"),
        Pair("=", "="),
        Pair("\n", ";"),
        Pair("out", ">")
)

val lexToCPP = hashMapOf(
        Pair(kwToLex["def"], "Number"),
        Pair(kwToLex["="], "="),
        Pair(kwToLex["\n"], ";\n\t"),
        Pair(kwToLex["out"], "cout <<"),
        Pair("|", "<< endl"),
        Pair("^", "%")
)

val operators = arrayOf("+", "-", "/", "*", "^", "=")
val cppFileName = "out.cpp"
val exampleCode = """
    def x = 2
    def y = 3
    y = y ^ x + 1
    out x + y
    """.trimIndent()

val startOfCppCode = """
    #include <iostream>
    #include <cmath>
    #include <ctgmath>
    using namespace std;

    class Number
    {
      public:
        double val;
        Number operator%(const Number& n) { return pow(val, n.val); }
        Number operator%(const double& d) { return pow(val, d); }

        Number operator*(const Number& n) { return val * n.val; }
        Number operator*(const double& d) { return val * d; }

        Number operator/(const Number& n) { return val / n.val; }
        Number operator/(const double& d) { return val / d; }

        Number operator+(const Number& n) { return val + n.val; }
        Number operator+(const double& d) { return val + d; }

        Number operator-(const Number& n) { return val - n.val; }
        Number operator-(const double& d) { return val - d; }

        Number(float val) { this->val = val; }
    };

    ostream &operator<<(ostream &os, const Number &num) { return os << num.val; }

    int main()
    {
    ${"\t"}
    """.trimIndent()

val endOfCPPCode = """
    ;
        return 0;
    }
    """.trimIndent()

fun isVarName(name: String) = name.matches("[a-zA-Z]+".toRegex())
fun isNumber(number: String) = number.matches("[0-9]+".toRegex())

fun toLexemeString(code: String): String
{
    var formattedCode = code.replace("\n", "\n")
    kwToLex.forEach { keyword, lexeme ->  formattedCode = formattedCode.replace(keyword, lexeme)}
    return formattedCode
}

fun isValidOperation(line: List<String>): Boolean
{
    var operatorCount = 0
    var operandCount = 0
    line.forEach {
        if (it in operators) operatorCount++
        else if (isVarName(it) || isNumber(it)) operandCount++
    }
    return operandCount > 0 && operatorCount + 1 == operandCount
}

fun lexemeStringIsValid(lexemeString: String): Boolean
{
    val lines = lexemeString.split(kwToLex["\n"]!!)
    lines // checking for syntax errors
            .map { it.split(" ") }
            .forEach {
                if (kwToLex["def"] in it) // should be in form: def varName = number
                {
                    if (!(it[0] == kwToLex["def"] && isVarName(it[1]) && it[2] == kwToLex["="] && isNumber(it[3]) && it.size == 4)) return false
                } else if (kwToLex["="] in it) // and it isn't a variable definition, so it has to be in the form x = x + 1 + a + ...
                {
                    if (!(isVarName(it[0]) && isValidOperation(it))) return false
                } else if (kwToLex["out"] in it) // should be in form out a + b - c / d * e ... Note how there there is always one more operand than operator, and more than 0 operands
                {
                    if (!(it[0] == kwToLex["out"] && isValidOperation(it))) return false
                } else { return false } // isnt a valid line
            }
    // now checking if all variables have been declared
    val lexemes = lexemeString.replace(kwToLex["\n"]!!, " ").split(" ")
    val declaredVars = ArrayList<String>()
    (0 until lexemes.size)
            .filter { lexemes[it] == kwToLex["def"] }
            .mapTo(declaredVars) { lexemes[it + 1] } // we know this is a valid declaration because the top bit would've checked for that
    lexemes.forEach { if (isVarName(it) && it !in declaredVars)  return false }

    return true
}

fun lexemesToCPPCode(lexemeString: String): String
{
    var convertedCode = lexemeString
    lexToCPP.forEach { lexeme, keyword -> convertedCode = convertedCode.replace(lexeme!!, keyword) }
    return "$startOfCppCode $convertedCode $endOfCPPCode"
}

fun writeCPPFile(cppCode: String) { File(cppFileName).bufferedWriter().use { out -> out.write(cppCode) } }

fun main(args: Array<String>)
{
    val lexemes = toLexemeString(exampleCode)
    if (lexemeStringIsValid(lexemes)) { writeCPPFile(lexemesToCPPCode(lexemes)) }
}