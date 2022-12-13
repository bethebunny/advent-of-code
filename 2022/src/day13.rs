#![feature(array_chunks)]

use std::{cmp::Ordering, num::ParseIntError};

mod aoc_client;

#[derive(PartialEq, Eq, Clone, Debug)]
enum PacketList {
    Value(usize),
    List(Vec<PacketList>),
}

use PacketList::{List, Value};

#[derive(Debug)]
enum Error {
    ParseIntError(ParseIntError),
    ParseError,
}

impl From<ParseIntError> for Error {
    fn from(e: ParseIntError) -> Error {
        Error::ParseIntError(e)
    }
}

impl PacketList {
    fn parse_list_with_len(s: &str) -> Result<(PacketList, usize), Error> {
        let bytes = s.as_bytes();
        if bytes[0] != b'[' {
            Err(Error::ParseError)?;
        }
        let mut len = 1;
        let mut depth = 1;
        while depth > 0 {
            match bytes[len] {
                b'[' => depth += 1,
                b']' => depth -= 1,
                _ => (),
            }
            len += 1; // Want to include last ] too
        }
        Ok((PacketList::parse(&s[1..len - 1])?, len))
    }

    fn parse_value_with_len(s: &str) -> Result<(PacketList, usize), Error> {
        let bytes = s.as_bytes();
        let mut len = 0;
        while len < bytes.len() && (b'0'..=b'9').contains(&bytes[len]) {
            len += 1;
        }
        Ok((Value(s[0..len].parse()?), len))
    }

    fn parse(s: &str) -> Result<PacketList, Error> {
        let bytes = s.as_bytes();
        let mut i = 0;
        let mut vals: Vec<PacketList> = vec![];
        while i < bytes.len() {
            if bytes[i] == b',' {
                i += 1;
                continue;
            }
            let (v, len) = (if bytes[i] == b'[' {
                PacketList::parse_list_with_len
            } else {
                PacketList::parse_value_with_len
            })(&s[i..])?;
            vals.push(v);
            i += len;
        }
        Ok(List(vals))
    }
}

impl Ord for PacketList {
    fn cmp(&self, other: &Self) -> Ordering {
        match (self, other) {
            (Value(l), Value(r)) => l.cmp(r),
            (List(_), Value(_)) => self.cmp(&List(vec![other.clone()])),
            (Value(_), List(_)) => List(vec![self.clone()]).cmp(other),
            (List(ll), List(rl)) => {
                let mut ords = ll.iter().zip(rl).map(|(l, r)| l.cmp(r));
                if let Some(ord) = ords.find(|&ord| ord != Ordering::Equal) {
                    ord
                } else {
                    ll.len().cmp(&rl.len())
                }
            }
        }
    }
}

impl PartialOrd for PacketList {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

fn main() {
    let data = aoc_client::input(2022, 13).unwrap();
    let raw_pairs: Vec<&str> = data.split("\n\n").collect();
    let pairs = raw_pairs
        .iter()
        .flat_map(|lines| {
            lines.trim().split_once("\n").map(|(p1, p2)| {
                (
                    PacketList::parse(&p1[1..p1.len() - 1]).unwrap(),
                    PacketList::parse(&p2[1..p2.len() - 1]).unwrap(),
                )
            })
        })
        .collect::<Vec<_>>();
    let in_order = pairs.iter().map(|(p1, p2)| p1.cmp(p2));
    println!(
        "{}",
        in_order
            .enumerate()
            .filter(|(_, ord)| *ord == Ordering::Less)
            .map(|(i, _)| i + 1)
            .sum::<usize>()
    );

    let mut all_packets: Vec<PacketList> = pairs
        .iter()
        .flat_map(|(p1, p2)| [p1, p2])
        .cloned()
        .collect();
    // Add divider packets
    let dividers = [
        List(vec![List(vec![Value(2)])]),
        List(vec![List(vec![Value(6)])]),
    ];
    all_packets.extend_from_slice(&dividers);
    all_packets.sort();
    println!(
        "{}",
        dividers
            .iter()
            .flat_map(|d| all_packets.iter().position(|p| p == d))
            .map(|x| x + 1) // 1-indexed
            .product::<usize>()
    );
}
