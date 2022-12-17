#![feature(iter_array_chunks)]
#![feature(iterator_try_collect)]

use std::collections::HashSet;
use std::num::ParseIntError;
use std::ops::RangeInclusive;

mod aoc_client;

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
struct P(i64, i64, i64);

impl std::ops::Add<P> for P {
    type Output = P;
    fn add(self, rhs: P) -> Self::Output {
        P(self.0 + rhs.0, self.1 + rhs.1, self.2 + rhs.2)
    }
}

static DIRECTIONS: [P; 6] =
    [P(0, 0, 1), P(0, 0, -1), P(0, 1, 0), P(0, -1, 0), P(1, 0, 0), P(-1, 0, 0)];

impl P {
    fn parse(s: &str) -> Result<Self, Error> {
        match s.split(",").array_chunks::<3>().next() {
            Some([x, y, z]) => Ok(P(x.parse()?, y.parse()?, z.parse()?)),
            _ => Err(Error::ParseError(s.to_string())),
        }
    }

    fn neighbors(&self) -> [P; 6] {
        DIRECTIONS.map(|d| d + *self)
    }
}

#[derive(Clone, Debug)]
struct Shape(HashSet<P>);

struct Bounds(RangeInclusive<i64>, RangeInclusive<i64>, RangeInclusive<i64>);

impl Bounds {
    fn contains(&self, p: &P) -> bool {
        self.0.contains(&p.0) && self.1.contains(&p.1) && self.2.contains(&p.2)
    }
}

impl Shape {
    fn parse(s: &str) -> Result<Self, Error> {
        Ok(Shape(s.lines().map(P::parse).try_collect()?))
    }

    fn surface_area(&self) -> usize {
        self.0.iter().flat_map(|p| p.neighbors()).filter(|p| !self.0.contains(p)).count()
    }

    fn interior_surface_area(&self) -> usize {
        let mut interior = self.0.clone();
        let bounds = Bounds(
            self.0.iter().map(|p| p.0).min().unwrap()..=self.0.iter().map(|p| p.0).max().unwrap(),
            self.0.iter().map(|p| p.1).min().unwrap()..=self.0.iter().map(|p| p.1).max().unwrap(),
            self.0.iter().map(|p| p.2).min().unwrap()..=self.0.iter().map(|p| p.2).max().unwrap(),
        );
        let points = self
            .0
            .iter()
            .flat_map(|p| p.neighbors())
            .filter(|p| !self.0.contains(p))
            .filter(|p| bounds.contains(p))
            .collect::<HashSet<_>>();
        let mut queue = points.clone();
        while let Some(&start) = queue.iter().next() {
            let mut is_maybe_interior = true;
            let mut flood: HashSet<P> = HashSet::from_iter(std::iter::once(start));
            let mut flood_queue = flood.clone();
            while let Some(&next) = flood_queue.iter().next() {
                flood_queue.remove(&next);
                debug_assert!(next == start || !flood.contains(&next));
                debug_assert!(!self.0.contains(&next));
                if !bounds.contains(&next) {
                    is_maybe_interior = false;
                    continue;
                }
                flood.insert(next);
                flood_queue.extend(
                    next.neighbors().iter().filter(|p| !flood.contains(p) && !self.0.contains(p)),
                );
            }
            if is_maybe_interior {
                interior.extend(flood.iter());
            }
            for checked in flood {
                queue.remove(&checked);
            }
        }

        self.0.iter().flat_map(|p| p.neighbors()).filter(|p| !interior.contains(p)).count()
    }
}

fn main() {
    let data = aoc_client::input(2022, 18).unwrap();
    let shape = Shape::parse(data.as_str()).unwrap();
    println!("{}", shape.surface_area());
    println!("{}", shape.interior_surface_area());
}
