import std;
import math;
import string_utils;

var str: string;

fun main(): int = {
    str = "SQRT:";
    writeString(str);
    writeInt(sqrt(49));
    writeInt(sqrt(101));

    str = "LOG:";
    writeString(str);
    writeInt(log(100));
    writeInt(log(1000));
    
    str = "RANDOM:";
    writeString(str);
    writeInt(random(5,100));
    writeInt(random(10,1000));

    0;
};