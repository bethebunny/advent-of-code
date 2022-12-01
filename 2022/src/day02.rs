mod aoc_client;

use std::collections::HashMap;
use std::hash::Hash;

fn counts<T: Eq + Hash + Copy, I: Iterator<Item=T>>(it: I) -> HashMap<T, u32> {
    let mut counts = HashMap::new();
    for v in it {
        counts.insert(v, counts.get(&v).unwrap_or(&0) + 1);
    }
    counts
}

fn checksum<'a, I>(it: I) -> u32
where
    I: Iterator<Item=&'a str>,
{
    let mut twos = 0;
    let mut threes = 0;
    for s in it {
        let counts = counts(s.chars());
        if counts.values().find(|c| **c == 2).is_some() {
            twos += 1;
        }
        if counts.values().find(|c| **c == 3).is_some() {
            threes += 1;
        }
    }
    twos * threes
}

fn diff_count(s1: &str, s2: &str) -> usize {
    s1.chars().zip(s2.chars()).filter(|(c1, c2)| c1 != c2).count()
}

fn pairs<'a, I>(it I) -> Iterator<(&'a str, &'a str)>
where
    I: Iterator<Item=&'a str>,
{
}

fn main() {
    let data = aoc_client::input(2018, 2).unwrap();
    let lines = data.trim().split("\n");
    println!("{:?}", counts("cheese".chars()));
    println!("{:?}", checksum(lines));
    println!("{:?}", diff_count("cheese", "crackers"));
}
