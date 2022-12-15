use std::cmp::max;
use std::collections::HashSet;
use std::{num::ParseIntError, ops::Range};

use core::fmt::Debug;

use lazy_static::lazy_static;
use regex::Regex;

mod aoc_client;

lazy_static! {
    static ref SENSOR_RE: Regex = Regex::new(r"Sensor at x=(?P<sensor_x>-?\d+), y=(?P<sensor_y>-?\d+): closest beacon is at x=(?P<beacon_x>-?\d+), y=(?P<beacon_y>-?\d+)").unwrap();
}

#[derive(Debug)]
enum Error {
    ParseIntError(ParseIntError),
    ParseError(String),
}

impl From<ParseIntError> for Error {
    fn from(e: ParseIntError) -> Error {
        Error::ParseIntError(e)
    }
}

#[derive(PartialEq, Eq, Hash, Clone, Copy, Debug)]
struct P(i64, i64);

#[derive(PartialEq, Eq, Hash, Clone, Copy, Debug)]
struct Sensor {
    loc: P,
    beacon: P,
}

#[derive(PartialEq, Eq, Hash, Clone, Debug)]
struct RangeSet<U>(Vec<Range<U>>);

impl P {
    fn manhattan(&self, other: &P) -> i64 {
        (other.0 - self.0).abs() + (other.1 - self.1).abs()
    }
}

fn ranges_overlap<U: Ord>(r1: &Range<U>, r2: &Range<U>) -> bool {
    if r2.start < r1.start {
        return ranges_overlap(r2, r1);
    }
    r1.end >= r2.start
}

fn join_overlapping_ranges<U: Ord + Copy>(r1: &Range<U>, r2: &Range<U>) -> Range<U> {
    if r2.start < r1.start {
        return join_overlapping_ranges(r2, r1);
    }
    assert!(ranges_overlap(r1, r2));
    r1.start..(max(r1.end, r2.end))
}

impl<U: Ord + Copy + Debug> RangeSet<U> {
    fn new() -> RangeSet<U> {
        RangeSet(vec![])
    }

    fn from_iter(it: impl Iterator<Item = Range<U>>) -> RangeSet<U> {
        let mut ranges = RangeSet::<U>::new();
        for range in it {
            ranges.insert(&range);
        }
        ranges
    }

    fn insert(&mut self, r: &Range<U>) {
        for i in 0..self.0.len() {
            let ir = &self.0[i];
            if ranges_overlap(r, ir) {
                let r = join_overlapping_ranges(r, ir);
                self.0.remove(i);
                self.insert(&r);
                return;
            } else if r.start < ir.start {
                self.0.insert(i, r.clone());
                return;
            }
        }
        self.0.push(r.clone());
    }

    fn contains(&self, v: &U) -> bool {
        self.0.iter().any(|r| r.contains(v))
    }

    fn contains_range(&self, r: &Range<U>) -> bool {
        for ir in self.0.iter() {
            if ir.contains(&r.start) {
                return ir.contains(&r.end) || ir.end == r.end;
            }
        }
        false
    }
}

impl RangeSet<i64> {
    fn len(&self) -> i64 {
        self.0.iter().map(|r| (r.end - r.start)).sum()
    }
}

impl Sensor {
    fn parse(s: &str) -> Result<Sensor, Error> {
        let caps = SENSOR_RE.captures(s).ok_or(Error::ParseError(s.to_string()))?;
        Ok(Sensor {
            loc: P(caps["sensor_x"].parse()?, caps["sensor_y"].parse()?),
            beacon: P(caps["beacon_x"].parse()?, caps["beacon_y"].parse()?),
        })
    }

    fn eliminated_locations_in_row(&self, y: i64) -> Option<Range<i64>> {
        let x = self.loc.0;
        let distance_to_row = (self.loc.1 - y).abs();
        let distance_to_beacon = self.loc.manhattan(&self.beacon);
        match distance_to_beacon - distance_to_row {
            dx if dx >= 0 => Some((x - dx)..(x + dx + 1)), // +1 so we don't need to use RangeInclusive
            _ => None,
        }
    }
}

fn eliminated_locations_in_row(sensors: &Vec<Sensor>, y: i64) -> RangeSet<i64> {
    RangeSet::from_iter(sensors.iter().flat_map(|s| s.eliminated_locations_in_row(y)))
}

fn main() {
    let data = aoc_client::input(2022, 15).unwrap();
    let sensors = data.lines().map(Sensor::parse).collect::<Result<Vec<_>, _>>().unwrap();

    let p1_row: i64 = 2_000_000;
    let p1_ranges = eliminated_locations_in_row(&sensors, p1_row);
    let beacons = sensors.iter().map(|s| s.beacon).collect::<HashSet<_>>();
    let beacon_positions =
        beacons.iter().filter(|b| b.1 == p1_row && p1_ranges.contains(&b.0)).count();
    println!("{}", p1_ranges.len() - beacon_positions as i64);

    for y in 0..=4_000_000 {
        let eliminated = eliminated_locations_in_row(&sensors, y);
        if !eliminated.contains_range(&(0..4_000_001)) {
            let x = (0..=4_000_000).find(|x| !eliminated.contains(x)).unwrap();
            println!("{}", x * 4_000_000 + y);
            break;
        }
    }
}
