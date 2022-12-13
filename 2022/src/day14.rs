#![feature(array_chunks)]

use std::{cmp::max, cmp::min, collections::HashMap};

mod aoc_client;

#[derive(Clone, Copy, Debug, PartialEq)]
#[repr(u8)]
enum Cell {
    Stone = b'#',
    Sand = b'o',
    Empty = b' ',
    Source = b'+',
}

#[derive(PartialEq, Eq, Hash, Clone, Copy, Debug)]
struct P(usize, usize);

impl P {
    fn parse(s: &str) -> P {
        let (rx, ry) = s.split_once(",").unwrap();
        P(rx.parse().unwrap(), ry.parse().unwrap())
    }

    fn range_to(&self, other: &P) -> Box<dyn Iterator<Item = P>> {
        if self.0 == other.0 {
            let x = self.0;
            Box::new((min(self.1, other.1)..=max(self.1, other.1)).map(move |y| P(x, y)))
        } else {
            let y = self.1;
            Box::new((min(self.0, other.0)..=max(self.0, other.0)).map(move |x| P(x, y)))
        }
    }

    fn points_below(&self) -> [P; 3] {
        [P(self.0, self.1 + 1), P(self.0 - 1, self.1 + 1), P(self.0 + 1, self.1 + 1)]
    }
}

struct Map(HashMap<P, Cell>);

static SOURCE: P = P(500, 0);

impl Map {
    fn parse(s: &str) -> Map {
        let mut gridpoints: HashMap<P, Cell> = HashMap::new();
        for line in s.lines() {
            let endpoints: Vec<P> = line.split(" -> ").map(P::parse).collect();
            for point_pair in endpoints.as_slice().windows(2) {
                match point_pair {
                    [start, end] => start.range_to(end).for_each(|p| {
                        gridpoints.insert(p, Cell::Stone);
                    }),
                    _ => panic!("Invalid map input"),
                }
            }
        }
        assert!(!gridpoints.contains_key(&SOURCE));
        gridpoints.insert(SOURCE, Cell::Source);
        Map(gridpoints)
    }

    fn draw(&self) {
        let min_x = self.0.keys().map(|p| p.0).min().unwrap();
        let max_x = self.0.keys().map(|p| p.0).max().unwrap();
        let min_y = self.0.keys().map(|p| p.1).min().unwrap();
        let max_y = self.0.keys().map(|p| p.1).max().unwrap();
        for y in min_y..=max_y {
            for x in min_x..=max_x {
                print!("{}", *self.0.get(&P(x, y)).unwrap_or(&Cell::Empty) as u8 as char);
            }
            println!();
        }
    }

    fn drop_one_sand(&mut self) -> bool {
        let mut sand_pos = SOURCE;
        let max_y = self.0.iter().filter(|(_, &c)| c == Cell::Stone).map(|p| p.0 .1).max().unwrap();
        let floor_y = max_y + 2;
        'outer: loop {
            //while sand_pos.1 < max_y {  // TODO: make part 1 still work but I'm too tired rn
            if sand_pos.1 < floor_y - 1 {
                for point_below in sand_pos.points_below() {
                    if !self.0.contains_key(&point_below) {
                        sand_pos = point_below;
                        continue 'outer;
                    }
                }
            }
            self.0.insert(sand_pos, Cell::Sand);
            return sand_pos != SOURCE;
        }
        false
    }

    fn drop_all_sand(&mut self) -> usize {
        let mut num_grains = 0;
        while self.drop_one_sand() {
            num_grains += 1;
        }
        num_grains
    }
}

fn main() {
    let data = aoc_client::input(2022, 14).unwrap();
    //     let data = "498,4 -> 498,6 -> 496,6
    // 503,4 -> 502,4 -> 502,9 -> 494,9"
    //         .to_string();
    let mut map = Map::parse(data.as_str());
    // For some reason, off by 1 for part 2
    println!("{}", map.drop_all_sand());
    map.draw();
}
