import std;
import math;

fun main() : int = ({
    putChar('N');putChar('?');putChar(' ');
    n = random(5, 15);
    nums = ((new (n * 8)) : ^int);

    i = 0;
    while i < n do
        getIdx(nums, i)^ = random(5, 50);
        i = i + 1;
    end;

    putChar((10 : char));
    putChar('S');putChar('o');putChar('r');
    putChar('t');putChar('e');putChar('d');
    putChar(' ');putChar('(');putChar('A');
    putChar('S');putChar('C');putChar(')');
    putChar(':');putChar((10 : char));
    nums = radix_sort(nums, n, 1);
    printNums(nums, n);

    putChar((10 : char));
    putChar('S');putChar('o');putChar('r');
    putChar('t');putChar('e');putChar('d');
    putChar(' ');putChar('(');putChar('D');
    putChar('E');putChar('S');putChar('C');
    putChar(')');putChar(':');putChar((10 : char));
    nums = radix_sort(nums, n, 0);
    printNums(nums, n);

    del (nums : ^void);

    0;
}where
    var i : int;
    var n : int;
    var nums : ^int;
    fun getIdx(arr : ^int, idx : int) : ^int = (((arr : int) + idx * 8) : ^int);
    fun printNums(arr : ^int, n : int) : void = ({
        putInt(getIdx(arr, 0)^);
        i = 1;
        while i < n do
            putChar(',');putChar(' ');
            putInt(getIdx(arr, i)^);
            i = i + 1;
        end;
        putChar((10 : char));
    } where
        var i : int;
    );
);

#{
#  Radix sort.
}#
fun radix_sort(arr : ^int, n : int, asc : int) : ^int = ({
    max = getIdx(arr, 0)^;

    i = 1;
    while i < n do
        if max < getIdx(arr, i)^ then
            max = getIdx(arr, i)^;
        end;
        i = i + 1;
    end;
    aux = ((new (8 * n)) : ^int);

    #{ Clear radix }#
    i = 0;
    while i < 10 do
        radix[i] = 0;
        i = i + 1;
    end;

    #{ Sort }#
    exp = 1;

    while max > 0 do
        i = 0;
        while i < n do
            r = (getIdx(arr, i)^ / exp) % 10;
            radix[r] = radix[r] + 1;
            i = i + 1;
        end;

        i = 1;
        while i < 10 & asc do
            radix[i] = radix[i] + radix[i - 1];
            i = i + 1;
        end;
        i = 9;
        while i > 0 & !asc do
            radix[i - 1] = radix[i - 1] + radix[i];
            i = i - 1;
        end;

        i = n - 1;
        while i >= 0 do
            r = (getIdx(arr, i)^ / exp) % 10;
            getIdx(aux, radix[r] - 1)^ = getIdx(arr, i)^;
            radix[r] = radix[r] - 1;
            i = i - 1;
        end;

        #{ Swap arr and aux }#
        r = (arr : int);
        arr = aux;
        aux = (r : ^int);

        #{ Clear radix. }#
        i = 0;
        while max > 9 & i < 10 do
            radix[i] = 0;
            i = i + 1;
        end;
        exp = exp * 10;
        max = max / 10;
    end;

    del (aux : ^void);
    arr;
}where
    var i : int;
    var r : int;
    var exp : int;
    var max : int;
    var aux : ^int;
    var radix : [10]int;
    fun getIdx(arr : ^int, idx : int) : ^int = (((arr : int) + idx * 8) : ^int);
);
