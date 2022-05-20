import std;

var num: int;
var num2: int;

fun main(): int = {
    num = -1;
    num2 = -1;

    num = getInt();
    num2 = getInt();

    writeInt(num);
    writeInt(num2);

    0;
};