use std::{collections::HashSet, num::ParseIntError};

mod aoc_client;

#[derive(Debug)]
enum Error {
    ParseIntError(ParseIntError),
    ParseDirectionError(String),
    SplitLineError(String),
}

type P = (i64, i64);

struct Rope<const N: usize> {
    knots: [P; N],
}

impl<const N: usize> Rope<N> {
    fn apply_move(&mut self, mv: P) {
        let knots = &mut self.knots;
        knots[0] = plus(knots[0], mv);
        for i in 1..N {
            knots[i] = plus(knots[i], tail_move(knots[i - 1], knots[i]));
        }
    }
}

fn plus(p1: P, p2: P) -> P {
    (p1.0 + p2.0, p1.1 + p2.1)
}

fn minus(p1: P, p2: P) -> P {
    (p1.0 - p2.0, p1.1 - p2.1)
}

fn tail_move(head_pos: P, tail_pos: P) -> P {
    let delta = minus(head_pos, tail_pos);
    if delta.0.abs() > 1 || delta.1.abs() > 1 {
        (delta.0.signum(), delta.1.signum())
    } else {
        (0, 0)
    }
}

fn parse_moves(line: &str) -> Result<Vec<P>, Error> {
    match line.split(" ").collect::<Vec<&str>>().as_slice() {
        [direction, distance] => {
            let distance: usize = distance.parse().map_err(Error::ParseIntError)?;
            let mv: P = match *direction {
                "U" => (0, 1),
                "D" => (0, -1),
                "L" => (-1, 0),
                "R" => (1, 0),
                _ => Err(Error::ParseDirectionError(direction.to_string()))?,
            };
            Ok(vec![mv; distance])
        }
        _ => Err(Error::SplitLineError(line.to_string())),
    }
}

fn main() {
    let data = aoc_client::input(2022, 9).unwrap();

    let moves = data
        .lines()
        .map(parse_moves)
        .collect::<Result<Vec<_>, _>>()
        .unwrap();

    let mut head_pos: P = (0, 0);
    let mut tail_pos: P = (0, 0);
    let mut tail_positions = HashSet::<P>::new();
    tail_positions.insert((0, 0));
    for mv in moves.iter().flatten() {
        head_pos = plus(head_pos, *mv);
        tail_pos = plus(tail_pos, tail_move(head_pos, tail_pos));
        tail_positions.insert(tail_pos);
    }
    println!("{}", tail_positions.len());

    let mut rope: Rope<10> = Rope {
        knots: [(0, 0); 10],
    };
    let mut tail_positions = HashSet::<P>::new();
    tail_positions.insert((0, 0));
    for mv in moves.iter().flatten() {
        rope.apply_move(*mv);
        tail_positions.insert(rope.knots[9]);
    }
    println!("{}", tail_positions.len());
}
