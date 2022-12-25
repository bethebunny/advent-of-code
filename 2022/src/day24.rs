#![feature(iterator_try_collect)]

use std::cmp::Ordering;
use std::collections::{BinaryHeap, HashMap, HashSet};
use std::ops::Add;

mod aoc_client;

#[derive(Debug)]
enum Error {
    InvalidCell(P, u8),
}

#[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
struct P(i64, i64);

impl P {
    fn manhattan(&self, other: &P) -> usize {
        ((self.0 - other.0).abs() + (self.1 - other.1).abs()) as usize
    }
}

impl Add<P> for P {
    type Output = P;
    fn add(self, rhs: P) -> Self::Output {
        P(self.0 + rhs.0, self.1 + rhs.1)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
struct Map {
    width: usize,
    height: usize,
    cells: HashMap<P, Cell>,
}

#[derive(Debug, PartialEq, Eq, Hash, Clone, Copy)]
enum Dir {
    North = 0,
    South = 1,
    East = 2,
    West = 3,
}

impl From<usize> for Dir {
    fn from(value: usize) -> Self {
        use Dir::*;
        match value {
            0 => North,
            1 => South,
            2 => East,
            3 => West,
            _ => panic!(),
        }
    }
}

#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
struct BlizzardDirs([bool; 4]);

impl From<Dir> for BlizzardDirs {
    fn from(value: Dir) -> Self {
        let mut dirs = BlizzardDirs::default();
        dirs.set(value);
        dirs
    }
}

impl BlizzardDirs {
    fn set(&mut self, dir: Dir) {
        self.0[dir as usize] = true;
    }

    fn iter(&self) -> BlizzardDirsIter {
        BlizzardDirsIter { dirs: self, next_dir: 0 }
    }

    fn draw(&self) -> char {
        let num_true = self.0.iter().map(|b| *b as u8).sum::<u8>();
        match num_true {
            0 => '.',
            1 => self.iter().next().unwrap().draw(),
            2 => '2',
            3 => '3',
            4 => '4',
            _ => unreachable!(),
        }
    }
}

struct BlizzardDirsIter<'a> {
    dirs: &'a BlizzardDirs,
    next_dir: usize,
}

impl<'a> Iterator for BlizzardDirsIter<'a> {
    type Item = Dir;
    fn next(&mut self) -> Option<Self::Item> {
        while self.next_dir < 4 {
            let dir = self.next_dir;
            self.next_dir = dir + 1;
            if self.dirs.0[dir] {
                return Some(dir.into());
            }
        }
        None
    }
}

impl Dir {
    fn as_vector(&self) -> P {
        use Dir::*;
        match self {
            North => P(0, -1),
            South => P(0, 1),
            East => P(1, 0),
            West => P(-1, 0),
        }
    }

    fn draw(&self) -> char {
        use Dir::*;
        match self {
            North => '^',
            South => 'v',
            East => '>',
            West => '<',
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Cell {
    Wall,
    Blizzard(BlizzardDirs),
}

impl Cell {
    fn from_byte(byte: u8) -> Option<Cell> {
        Some(match byte {
            b'#' => Cell::Wall,
            b'.' => Cell::Blizzard(BlizzardDirs::default()),
            b'>' => Cell::Blizzard(Dir::East.into()),
            b'<' => Cell::Blizzard(Dir::West.into()),
            b'v' => Cell::Blizzard(Dir::South.into()),
            b'^' => Cell::Blizzard(Dir::North.into()),
            _ => None?,
        })
    }

    fn draw(&self) -> char {
        match self {
            Cell::Wall => '#',
            Cell::Blizzard(dirs) => dirs.draw(),
        }
    }
}

impl Map {
    fn parse(s: &str) -> Result<Map, Error> {
        let mut cells = HashMap::new();
        for (y, line) in s.lines().enumerate() {
            for (x, byte) in line.bytes().enumerate() {
                let p = P(x as i64, y as i64);
                if let Some(cell) = Cell::from_byte(byte) {
                    cells.insert(p, cell);
                } else {
                    Err(Error::InvalidCell(p, byte))?;
                }
            }
        }
        let width = cells.keys().map(|p| p.0 as usize).max().unwrap() + 1;
        let height = cells.keys().map(|p| p.1 as usize).max().unwrap() + 1;
        Ok(Map { cells, width, height })
    }

    fn can_move_to(&self, p: &P) -> bool {
        (0..self.width as i64).contains(&p.0)
            && (0..self.height as i64).contains(&p.1)
            && !self.cells.contains_key(p)
    }

    fn step(&self) -> Map {
        let mut next_cells: HashMap<P, Cell> = HashMap::new();
        for (p, cell) in self.cells.iter() {
            match cell {
                Cell::Wall => {
                    next_cells.insert(*p, *cell);
                }
                Cell::Blizzard(dirs) => dirs.iter().for_each(|dir| {
                    let neighbor = *p + dir.as_vector();
                    // Wrap around if necessary
                    let neighbor = match neighbor {
                        P(0, y) => P(self.width as i64 - 2, y),
                        P(x, 0) => P(x, self.height as i64 - 2),
                        P(x, y) if x == self.width as i64 - 1 => P(1, y),
                        P(x, y) if y == self.height as i64 - 1 => P(x, 1),
                        _ => neighbor,
                    };
                    let neighbor_cell =
                        next_cells.entry(neighbor).or_insert(Cell::Blizzard(Default::default()));
                    match neighbor_cell {
                        Cell::Blizzard(dirs) => dirs.set(dir),
                        Cell::Wall => unreachable!(),
                    };
                }),
            }
        }

        Map { cells: next_cells, width: self.width, height: self.height }
    }

    #[allow(dead_code)]
    fn draw(&self, pos: P) {
        let empty = Cell::Blizzard(Default::default());
        let cyan = "\x1B[36m";
        let white = "\x1B[37m";
        let red = "\x1B[91m";
        let reset = "\x1B[0m";
        for y in 0..self.height as i64 {
            for x in 0..self.width as i64 {
                if P(x, y) == pos {
                    print!("{red}E{reset}");
                } else {
                    if let Some(cell) = self.cells.get(&P(x, y)) {
                        print!("{cyan}{}{reset}", cell.draw());
                    } else {
                        print!("{white}{}{reset}", empty.draw());
                    }
                }
            }
            println!();
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Hash)]
struct State {
    pos: P,
    ts: usize,
    goal: P, // Required for priority queue
}

impl State {
    fn next(&self, next_map: &Map) -> Vec<State> {
        let ts = self.ts + 1;
        [P(0, 0), P(0, 1), P(0, -1), P(1, 0), P(-1, 0)]
            .iter()
            .flat_map(|delta| {
                let pos = self.pos + *delta;
                // WHY rustfmt
                if next_map.can_move_to(&pos) {
                    Some(pos)
                } else {
                    None
                }
            })
            .map(|pos| State { pos, ts, goal: self.goal })
            .collect()
    }

    fn heuristic(&self) -> i64 {
        (self.ts + self.pos.manhattan(&self.goal)) as i64
    }
}

// Required for Ord
impl PartialOrd for State {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for State {
    fn cmp(&self, other: &Self) -> Ordering {
        // BinaryHeap is a max-heap, so we want to invert the ordering
        let h = -self.heuristic();
        let oh = -other.heuristic();
        let d = -(self.pos.manhattan(&self.goal) as i64);
        let od = -(other.pos.manhattan(&other.goal) as i64);
        if h != oh {
            h.cmp(&oh)
        } else {
            d.cmp(&od)
        }
    }
}

fn find_best_path(map: &Map, state: State) -> usize {
    let cycle_length = (map.height - 2) * (map.width - 2);

    // Compute all possible blizzard maps
    let maps: Vec<Map> = (0..cycle_length)
        .scan(map.clone(), |map, _| {
            let r = map.clone();
            let next_map = map.step();
            *map = next_map;
            Some(r)
        })
        .collect();

    let mut queue = BinaryHeap::new();
    let mut seen = HashSet::new();
    queue.push(state);
    let mut best_time = std::usize::MAX;
    while let Some(state) = queue.pop() {
        if seen.contains(&state) {
            continue;
        }
        seen.insert(state);
        if state.heuristic() as usize >= best_time {
            break; // we've considered all states that can possibly beat the best time!
        }
        if state.pos == state.goal {
            best_time = std::cmp::min(best_time, state.ts);
        } else {
            queue.extend(state.next(&maps[(state.ts + 1) % cycle_length]).drain(..))
        }
    }
    best_time
}

fn main() {
    let data = aoc_client::input(2022, 24).unwrap();
    let map = Map::parse(data.as_str()).unwrap();
    let start = P(1, 0);
    let end = P(map.width as i64 - 2, map.height as i64 - 1);
    let ts = find_best_path(&map, State { pos: start, goal: end, ts: 0 });
    println!("{ts}");
    let ts = find_best_path(&map, State { pos: end, goal: start, ts });
    let ts = find_best_path(&map, State { pos: start, goal: end, ts });
    println!("{ts}");
}
