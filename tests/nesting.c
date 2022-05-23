var nums: [10]int;

fun main(): int = ({
    nums[2] = 10;
    add4(10,20);
}   where 
        var local1: int;
        fun add4(par1: int, par2: int): int = (
            add4_(par1, par2, local1, 40)
            where
                fun add4_(a: int, b: int, c: int, d: int): int = ({
                    c = 30;
                    add_2(add_2(a,b), add_2(c,d));
                } 
                where fun add_2(a: int, b: int): int = a + b + nums[2];);
        );
);