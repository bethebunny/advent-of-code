#![feature(iterator_try_collect)]

use std::collections::{HashMap, HashSet};
use std::ops::Add;

mod aoc_client;

#[derive(Debug)]
enum Error {
    InvalidCell(P, u8),
}

#[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
struct P(i64, i64);

static ALL_DIRECTIONS: [P; 8] =
    [P(-1, -1), P(-1, 0), P(-1, 1), P(0, -1), P(0, 1), P(1, -1), P(1, 0), P(1, 1)];

impl P {
    fn neighbors(&self) -> [P; 8] {
        ALL_DIRECTIONS.map(|d| d + *self)
    }
}

impl Add<P> for P {
    type Output = P;
    fn add(self, rhs: P) -> Self::Output {
        P(self.0 + rhs.0, self.1 + rhs.1)
    }
}

#[derive(Debug, Clone)]
struct Map {
    elves: HashSet<P>,
    proposal_order: [Dir; 4],
}

#[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
enum Dir {
    North,
    South,
    East,
    West,
}

impl Dir {
    fn deltas(&self) -> [P; 3] {
        match self {
            Self::North => [P(0, -1), P(1, -1), P(-1, -1)], // N, NE, NW
            Self::South => [P(0, 1), P(1, 1), P(-1, 1)],    // S, SE, SW
            Self::West => [P(-1, 0), P(-1, -1), P(-1, 1)],  // W, NW, SW
            Self::East => [P(1, 0), P(1, -1), P(1, 1)],     // E, NE, SE
        }
    }

    fn as_vector(&self) -> P {
        self.deltas()[0]
    }
}

impl Map {
    fn parse(s: &str) -> Result<Map, Error> {
        let mut elves = HashSet::new();
        for (y, line) in s.lines().enumerate() {
            for (x, byte) in line.bytes().enumerate() {
                let p = P(x as i64, y as i64);
                match byte {
                    b'#' => elves.insert(p),
                    b'.' => false,
                    _ => Err(Error::InvalidCell(p, byte))?,
                };
            }
        }
        Ok(Map { elves, proposal_order: [Dir::North, Dir::South, Dir::West, Dir::East] })
    }

    fn step(&self) -> Map {
        let mut proposals: HashMap<P, Vec<P>> = HashMap::new();
        for &elf in self.elves.iter() {
            let best_proposal = self
                .proposal_order
                .iter()
                .filter(|d| !d.deltas().iter().any(|&v| self.elves.contains(&(v + elf))))
                .next();
            let proposal = if !elf.neighbors().iter().any(|n| self.elves.contains(n)) {
                elf
            } else if let Some(dir) = best_proposal {
                dir.as_vector() + elf
            } else {
                elf
            };
            proposals.entry(proposal).or_default().push(elf);
        }

        let mut proposal_order = self.proposal_order.clone();
        proposal_order.rotate_left(1);

        let elves: HashSet<_> = proposals
            .into_iter()
            .flat_map(|(k, v)| if v.len() == 1 { vec![k] } else { v })
            .collect();

        assert!(elves.len() == self.elves.len());

        Map { elves, proposal_order }
    }

    #[allow(dead_code)]
    fn draw(&self) {
        let min_x = self.elves.iter().map(|p| p.0).min().unwrap();
        let max_x = self.elves.iter().map(|p| p.0).max().unwrap();
        let min_y = self.elves.iter().map(|p| p.1).min().unwrap();
        let max_y = self.elves.iter().map(|p| p.1).max().unwrap();
        for y in min_y..=max_y {
            for x in min_x..=max_x {
                let c = if self.elves.contains(&P(x, y)) { '#' } else { '.' };
                print!("{c}");
            }
            println!();
        }
    }

    fn empty_tiles(&self) -> usize {
        let min_x = self.elves.iter().map(|p| p.0).min().unwrap();
        let max_x = self.elves.iter().map(|p| p.0).max().unwrap();
        let min_y = self.elves.iter().map(|p| p.1).min().unwrap();
        let max_y = self.elves.iter().map(|p| p.1).max().unwrap();
        (max_x - min_x + 1) as usize * (max_y - min_y + 1) as usize - self.elves.len()
    }
}

fn main() {
    let data = aoc_client::input(2022, 23).unwrap();
    let map = Map::parse(data.as_str()).unwrap();
    println!("{}", (0..10).fold(map, |m, _| m.step()).empty_tiles());

    let mut map = Map::parse(data.as_str()).unwrap();
    let mut round = 0;
    loop {
        round += 1;
        let next_map = map.step();
        if next_map.elves == map.elves {
            break;
        }
        map = next_map;
    }
    println!("{round}");
}
