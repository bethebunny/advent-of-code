use std::{collections::HashSet, hash::Hash};

mod aoc_client;

#[derive(Clone, Copy, PartialEq, Eq, Hash)]
struct P {
    x: usize,
    y: usize,
}
struct Map(Vec<Vec<u8>>);

impl Map {
    fn heights(&self) -> Vec<(P, u8)> {
        self.0
            .iter()
            .enumerate()
            .flat_map(|(y, line)| line.iter().enumerate().map(move |(x, &h)| (P { x, y }, h)))
            .collect()
    }

    fn height_at(&self, p: P) -> u8 {
        match self.0[p.y][p.x] {
            b'S' => b'a',
            b'E' => b'z',
            b => b,
        }
    }

    fn height(&self) -> usize {
        self.0.len()
    }

    fn width(&self) -> usize {
        self.0[0].len()
    }

    fn start(&self) -> Result<P, ()> {
        match self.heights().iter().find(|(_, h)| *h == b'S') {
            Some((p, _)) => Ok(*p),
            None => Err(()),
        }
    }

    fn end(&self) -> Result<P, ()> {
        match self.heights().iter().find(|(_, h)| *h == b'E') {
            Some((p, _)) => Ok(*p),
            None => Err(()),
        }
    }

    fn neighbors(&self, p: P) -> Vec<P> {
        [(0, 1), (0, -1), (-1, 0), (1, 0)]
            .iter()
            .flat_map(|(dx, dy)| {
                let x = p.x as i64 + dx;
                let y = p.y as i64 + dy;
                if x >= 0 && x < self.width() as i64 && y >= 0 && y < self.height() as i64 {
                    Some(P {
                        x: x as usize,
                        y: y as usize,
                    })
                } else {
                    None
                }
            })
            .collect()
    }

    fn can_move_from(&self, p1: P, p2: P) -> bool {
        self.height_at(p2) <= self.height_at(p1) + 1
    }

    fn shortest_paths(&self) -> Vec<Vec<usize>> {
        let mut shortest_paths = vec![vec![std::usize::MAX; self.width()]; self.height()];
        let end = self.end().unwrap();
        shortest_paths[end.y][end.x] = 0;
        let mut queue = vec![end];
        while let Some(p) = queue.pop() {
            let path = shortest_paths[p.y][p.x];
            for neighbor in self.neighbors(p) {
                if self.can_move_from(neighbor, p)
                    && shortest_paths[neighbor.y][neighbor.x] > path + 1
                {
                    shortest_paths[neighbor.y][neighbor.x] = path + 1;
                    queue.push(neighbor);
                }
            }
        }
        shortest_paths
    }

    fn shortest_path(&self) -> Vec<P> {
        let shortest_paths = self.shortest_paths();
        let start = self.start().unwrap();
        let end = self.end().unwrap();
        shortest_paths[start.y][start.x];
        let mut path = vec![start];
        while let Some(&l) = path.last() {
            if l == end {
                break;
            }
            let pathlen = shortest_paths[l.y][l.x];
            'inner: for n in self.neighbors(l) {
                if shortest_paths[n.y][n.x] == pathlen - 1 {
                    path.push(n);
                    break 'inner;
                }
            }
        }
        path
    }

    fn draw_path(&self, path: &Vec<P>) {
        let white = "\x1B[37m";
        let green = "\x1B[32m";
        let reset = "\x1B[0m";
        let path = path.iter().collect::<HashSet<_>>();
        for (y, line) in self.0.iter().enumerate() {
            for (x, b) in line.iter().enumerate() {
                let color = if path.contains(&P { x, y }) {
                    green
                } else {
                    white
                };
                print!("{color}{}{reset}", *b as char);
            }
            println!();
        }
    }
}

fn main() {
    let data = aoc_client::input(2022, 12).unwrap();
    let map = Map(data.lines().map(|line| line.bytes().collect()).collect());
    let shortest_path = map.shortest_path();
    map.draw_path(&shortest_path);
    println!("{}", shortest_path.len() - 1); // includes start

    let shortest_paths = map.shortest_paths();
    let heights = map.heights();
    let starting_points = heights.iter().filter(|(_, h)| *h == b'a');
    let best_hike: usize = starting_points
        .map(|(p, _)| shortest_paths[p.y][p.x])
        .min()
        .unwrap();
    println!("{}", best_hike);
}
