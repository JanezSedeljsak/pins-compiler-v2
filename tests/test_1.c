import std;

fun main(): int = {
    num = 0;
    while num < 10 do
        arr[num] = (num + 65 : char);
        num = num + 1;
    end;

    num = 0;
    while num < 10 do
        putChar(arr[num]);
        putChar((10 : char));
        num = num + 1;
    end;

    writeInt(100);
    0;
};

var num: int;
var arr: [10]char;