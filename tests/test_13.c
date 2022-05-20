fun quick(array : ^[50]int, low : int, high : int) : void = ({
    if (low < high) then
        mid = partition(array, low, high);
        quick(array, low, mid - 1);
        quick(array, mid + 1, high);
    end;
}
where
    var mid : int;
);

fun partition(array : ^[50]int, low : int, high : int) : int = ({
    pivot = array^[high];
    i = low - 1;
    j = low;
    while (j < high) do
        if (array^[j] <= pivot) then
            i = i + 1;
            swap(array, i, j);
        end;
        j = j + 1;
    end;
    swap(array, i + 1, high);
    i + 1;
}
where
    var i : int;
    var j : int;
    var pivot : int;
);

fun swap(array : ^[50]int, i : int, j : int) : void = ({
    temp = array^[i];
    array^[i] = array^[j];
    array^[j] = temp;
    none;
}
where
    var temp : int;
);

fun fill(array : ^[50]int, size : int) : void = ({
    i = 0;
    while (i < size) do
        array^[i] = random(5, 25);
        i = i + 1;
    end;
}
where
    var i : int;
    var rand : int;
);

fun printArray(array : ^[50]int, size : int) : void = ({
    i = 0;
    while (i < size) do
        putInt((array^)[i]);
        putChar(' ');
        i = i + 1;
    end;
    putChar(endl());
}
where
    var i : int;
);

fun endl() : char = (10 : char);

var array : [50]int;

fun main() : void = {
    fill(^array, 50);
    printArray(^array, 50);
    quick(^array, 0, 49);
    printArray(^array, 50);
    none;
};

#{ Forward declarations }#
fun putChar(c : char) : void = none;
fun getChar() : char = (0 : char);
fun putInt(i : int) : void = none;
fun getInt() : int = 0;
fun random(min : int, max : int) : int = 0;