typ string = ^char;

#{ ugliest string constructor ever but it works }#
fun make_string(c0: char, c1: char, c2: char, c3: char, c4: char, c5: char, c6: char, c7: char, c8: char, c9: char,
                c10: char, c11: char, c12: char, c13: char, c14: char, c15: char, c16: char, c17: char, c18: char, c19: char,
                c20: char, c21: char, c22: char, c23: char, c24: char, c25: char, c26: char, c27: char, c28: char, c29: char): string = ({

    ptr = new 240;
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
    (((ptr : int) + 10*8): ^char)^ = c10;
    (((ptr : int) + 11*8): ^char)^ = c11;
    (((ptr : int) + 12*8): ^char)^ = c12;
    (((ptr : int) + 13*8): ^char)^ = c13;
    (((ptr : int) + 14*8): ^char)^ = c14;
    (((ptr : int) + 15*8): ^char)^ = c15;
    (((ptr : int) + 16*8): ^char)^ = c16;
    (((ptr : int) + 17*8): ^char)^ = c17;
    (((ptr : int) + 18*8): ^char)^ = c18;
    (((ptr : int) + 19*8): ^char)^ = c19;
    (((ptr : int) + 20*8): ^char)^ = c20;
    (((ptr : int) + 21*8): ^char)^ = c21;
    (((ptr : int) + 22*8): ^char)^ = c22;
    (((ptr : int) + 23*8): ^char)^ = c23;
    (((ptr : int) + 24*8): ^char)^ = c24;
    (((ptr : int) + 25*8): ^char)^ = c25;
    (((ptr : int) + 26*8): ^char)^ = c26;
    (((ptr : int) + 27*8): ^char)^ = c27;
    (((ptr : int) + 28*8): ^char)^ = c28;
    (((ptr : int) + 29*8): ^char)^ = c29;

    ptr;

} where 
    var ptr: string;
);

fun getString(): string = ({
    ptr = new 160;
    index = 0;
    c = getChar();

    while index < 29 & c != (10: char) do
        str_adr(ptr, index)^ = c;
        c = getChar();
        index = index + 1;
    end;

    str_adr(ptr, index)^ = (10: char);
    ptr;
} where
    var ptr : string;
    var c : char;
    var index: int;
);

fun concat(str1: string, str2: string): string = ({
    ptr = new 160;
    len1 = len(str1);
    len2 = len(str2);

    new_index = 0;
    index = 0;

    while index < len1 & new_index < 29 do
        str_adr(ptr, new_index)^ = str_adr(str1, index)^;
        index = index + 1;
        new_index = new_index + 1;
    end;

    index = 0;
    while index < len2 & new_index < 29 do
        str_adr(ptr, new_index)^ = str_adr(str2, index)^;
        index = index + 1;
        new_index = new_index + 1;
    end;

    str_adr(ptr, new_index)^ = (10: char);
    ptr;

} where
    var ptr: string;
    var index: int;
    var new_index: int;
    var len1: int;
    var len2: int;
);

fun putString(str: string): void = ({
    index = 0;
    temp = str^;
    while temp != (10: char) & index < 30 do
        putChar(temp);
        index = index + 1;
        temp = (((str : int) + index*8): ^char)^;
    end;
} where
    var index: int;
    var temp: char;
);

fun writeString(str: string): void = {
    putString(str);
    endl();
};

fun log_str(str: string): void = ({
    index = 0;
    temp = str^;
    while index < 30 do
        putInt((temp: int));
        putChar(' ');
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
    while temp != (10: char) & index < 30 do
        index = index + 1;
        temp = (((str : int) + index*8): ^char)^;
    end;

    index; 
} where 
    var index: int;
    var temp: char;
);

fun str_adr(ptr: string, i: int): ^char = (((ptr: int) + i*8): ^char);