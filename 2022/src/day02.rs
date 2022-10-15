#[macro_use]
extern crate lazy_static;

use std::collections::HashMap;

mod aoc_client;

lazy_static! {
    static ref ROUND_SCORES_PART_1: HashMap<&'static str, u32> = HashMap::from([
        ("A X", 4),
        ("A Y", 8),
        ("A Z", 3),
        ("B X", 1),
        ("B Y", 5),
        ("B Z", 9),
        ("C X", 7),
        ("C Y", 2),
        ("C Z", 6),
    ]);
    static ref ROUND_SCORES_PART_2: HashMap<&'static str, u32> = HashMap::from([
        ("A X", 3),
        ("A Y", 4),
        ("A Z", 8),
        ("B X", 1),
        ("B Y", 5),
        ("B Z", 9),
        ("C X", 2),
        ("C Y", 6),
        ("C Z", 7),
    ]);
}

fn main() {
    let data = aoc_client::input(2022, 2).unwrap();
    println!(
        "{}",
        data.lines().map(|s| ROUND_SCORES_PART_1[s]).sum::<u32>()
    );
    println!(
        "{}",
        data.lines().map(|s| ROUND_SCORES_PART_2[s]).sum::<u32>()
    );
}
