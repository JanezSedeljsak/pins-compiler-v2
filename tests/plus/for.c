import std;
import string;
import math;

fun isPrime(num: int): int = ({
    for {i = 2; result = 1;}; i <= sqrt(num) & result != 0; i = i + 1; do
        if num % i == 0 then
            result = 0;
        end;
    end;

    result;
}   where var i: int; var result: int;);

fun main(): int = ({
    str = "Izpis prastevil do 100: ";
    putString(str);

    str = "";
    count = 0;

    for i = 2; i <= 100; i = i + 1; do
        if isPrime(i) then
            count = count + 1;
            putString(str);
            putInt(i);
            str = ", ";
        end;
    end;

    endl();
    str = "Blo jih je: ";
    putString(str);
    writeInt(count);

    0;
}   where var str: string; var count: int; var i: int;);