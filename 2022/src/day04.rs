mod aoc_client;

use regex::{Match, Regex};
use std::ops::RangeInclusive;

struct AssignmentPair([RangeInclusive<u32>; 2]);

impl AssignmentPair {
    fn from_str(s: &str) -> AssignmentPair {
        let regex = Regex::new(r"(\d+)-(\d+),(\d+)-(\d+)").unwrap();
        let groups = regex.captures(s).unwrap();
        AssignmentPair([
            // Ranges are [), assignments are []
            parse_match_int(groups.get(1)).unwrap()..=parse_match_int(groups.get(2)).unwrap(),
            parse_match_int(groups.get(3)).unwrap()..=parse_match_int(groups.get(4)).unwrap(),
        ])
    }

    fn one_contains_other(&self) -> bool {
        let [left, right] = &self.0;
        (left.start() <= right.start() && left.end() >= right.end())
            || (right.start() <= left.start() && right.end() >= left.end())
    }

    fn overlapping(&self) -> bool {
        let [left, right] = &self.0;
        // If overlapping, one will always contain the other's start
        left.contains(&right.start()) || right.contains(&left.start())
    }
}

fn parse_match_int(m: Option<Match>) -> Option<u32> {
    match m {
        Some(d) => d.as_str().parse::<u32>().ok(),
        None => None,
    }
}

fn main() {
    let data = aoc_client::input(2022, 4).unwrap();
    let trivial_assignment_count = data
        .lines()
        .map(AssignmentPair::from_str)
        .filter(|a| a.one_contains_other())
        .count();
    println!("{}", trivial_assignment_count);
    let overlapping_assignment_count = data
        .lines()
        .map(AssignmentPair::from_str)
        .filter(|a| a.overlapping())
        .count();
    println!("{}", overlapping_assignment_count);
}
