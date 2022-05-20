typ string = ^char;

fun make_string(c0: char, c1: char, c2: char, c3: char, c4: char, c5: char, c6: char, c7: char, c8: char, c9: char): string = ({
    ptr = new 80;
    ptr^ = c0;
    (((ptr : int) + 1*8): ^char)^ = c1;
    (((ptr : int) + 2*8): ^char)^ = c2;
    (((ptr : int) + 3*8): ^char)^ = c3;
    (((ptr : int) + 4*8): ^char)^ = c4;
    (((ptr : int) + 5*8): ^char)^ = c5;
    (((ptr : int) + 6*8): ^char)^ = c6;
    (((ptr : int) + 7*8): ^char)^ = c7;
    (((ptr : int) + 8*8): ^char)^ = c8;
    (((ptr : int) + 9*8): ^char)^ = c9;

    ptr;

} where 
    var ptr: ^char;
);

fun writeString(str: string): void = ({
    index = 0;
    temp = str^;
    while temp != (10: char) & index < 10 do
        putChar(temp);
        index = index + 1;
        temp = (((str : int) + index*8): ^char)^;
    end;

    endl();

} where
    var index: int;
    var temp: char;
);

fun len(str: string): int = ({
    index = 0;
    temp = str^;
    while temp != (10: char) & index < 10 do
        index = index + 1;
        temp = (((str : int) + index*8): ^char)^;
    end;

    index; 
} where 
    var index: int;
    var temp: char;
);