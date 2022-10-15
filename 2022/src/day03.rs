#![feature(iter_array_chunks)]

mod aoc_client;

use std::collections::HashSet;

fn main() {
    let data = aoc_client::input(2022, 3).unwrap();

    let shared_item_priority_sum: u32 = data
        .lines()
        .map(split_sack)
        .map(|halves| shared_items(&halves))
        .map(|items| *items.iter().next().unwrap())
        .map(item_priority)
        .map(u32::from)
        .sum();
    println!("{}", shared_item_priority_sum);

    let elf_groups = data.lines().array_chunks::<3>();
    let badge_priority_sum: u32 = elf_groups
        .map(|sacks| *shared_items(&sacks).iter().next().unwrap())
        .map(item_priority)
        .map(u32::from)
        .sum();
    println!("{}", badge_priority_sum);
}

fn split_sack(sack: &str) -> [&str; 2] {
    [&sack[..sack.len() / 2], &sack[sack.len() / 2..]]
}

fn shared_items(rucksacks: &[&str]) -> HashSet<u8> {
    match rucksacks.len() {
        0 => HashSet::new(),
        1 => HashSet::from_iter(rucksacks[0].bytes()),
        _ => {
            let left_unique: HashSet<u8> = HashSet::from_iter(rucksacks[0].bytes());
            let rest_shared = shared_items(&rucksacks[1..]);
            left_unique.intersection(&rest_shared).copied().collect()
        }
    }
}

fn item_priority(item: u8) -> u8 {
    if item >= b'a' {
        item - b'a' + 1
    } else {
        item - b'A' + 27
    }
}
