import std;
import string;

var num: int;
var num2: int;

var str: string;

fun main(): int = {
    str = "Vnesi a:";
    writeString(str);
    num = getInt();

    str = "Vnesi b:";
    writeString(str);
    num2 = getInt();

    str = "a + b =";
    writeString(str);
    writeInt(add(num, num2));

    0;
};

fun add(a: int, b: int): int = a + b;