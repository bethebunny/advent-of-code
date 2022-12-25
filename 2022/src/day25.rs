#![feature(iterator_try_collect)]

mod aoc_client;

fn snafu_d2i(d: char) -> i8 {
    match d {
        '=' => -2,
        '-' => -1,
        '0' => 0,
        '1' => 1,
        '2' => 2,
        _ => panic!("Invalid snafu digit {d}"),
    }
}

fn snafu_i2d(d: i8) -> char {
    assert!((-2..=2).contains(&d));
    match d {
        2 => '2',
        1 => '1',
        0 => '0',
        -1 => '-',
        -2 => '=',
        _ => unreachable!(),
    }
}

fn parse_snafu(s: &str) -> Result<i64, String> {
    Ok(s.chars()
        .rev()
        .enumerate()
        .map(|(place, d)| 5i64.pow(place as u32) * snafu_d2i(d) as i64)
        .sum::<i64>())
}

fn snafu(mut v: i64) -> String {
    let mut digits = vec![];
    let mut place = 0;
    while v != 0 {
        let c = 5i64.pow(place as u32);
        let pv = v % (c * 5);
        let digit = pv / c; // [0, 5)
        let digit = (digit + 2) % 5 - 2; // [-2, 2)
        digits.push(digit as i8);
        v -= digit * c;
        place += 1;
    }
    digits.iter().rev().copied().map(snafu_i2d).collect()
}

fn main() {
    let data = aoc_client::input(2022, 25).unwrap();
    let fuel_amounts: Vec<i64> = data.lines().map(parse_snafu).try_collect().unwrap();
    println!("{}", snafu(fuel_amounts.iter().sum()));
}
