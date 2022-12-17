use std::collections::{HashMap, HashSet};

mod aoc_client;

#[derive(Clone, Copy, Debug)]
enum JetDir {
    Left,
    Right,
}

#[derive(PartialEq, Eq, Hash, Clone, Copy, Debug)]
struct P(i64, i64);

impl std::ops::Add<P> for P {
    type Output = P;
    fn add(self, rhs: P) -> Self::Output {
        P(self.0 + rhs.0, self.1 + rhs.1)
    }
}

#[derive(Debug, Clone)]
struct Shape {
    parts: Vec<P>,
}

impl Shape {
    fn all() -> [Shape; 5] {
        [
            Shape { parts: vec![P(0, 0), P(1, 0), P(2, 0), P(3, 0)] },
            Shape { parts: vec![P(1, 0), P(0, 1), P(1, 1), P(2, 1), P(1, 2)] },
            Shape { parts: vec![P(0, 0), P(1, 0), P(2, 0), P(2, 1), P(2, 2)] },
            Shape { parts: vec![P(0, 0), P(0, 1), P(0, 2), P(0, 3)] },
            Shape { parts: vec![P(0, 0), P(1, 0), P(0, 1), P(1, 1)] },
        ]
    }

    fn width(&self) -> usize {
        self.parts.iter().map(|p| p.0 as usize).max().unwrap() + 1
    }

    fn points_at_pos(&self, p: &P) -> Vec<P> {
        self.parts.iter().map(|&point| point + *p).collect()
    }
}

#[derive(Clone, Debug)]
struct Map(HashSet<P>);

impl Map {
    fn new() -> Map {
        Map(HashSet::from_iter((0..7).map(|x| P(x, 0))))
    }

    fn height(&self) -> usize {
        self.0.iter().map(|p| p.1 as usize).max().unwrap()
    }

    fn spawn_shape(
        &mut self,
        shape: Shape,
        jets: &mut impl Iterator<Item = (usize, JetDir)>,
    ) -> usize {
        let mut shape_pos = P(2, self.height() as i64 + 3 + 1); // + 1 for reasons
        let mut dir_index = 0;
        while let Some((i, dir)) = jets.next() {
            dir_index = i;
            let try_pos = match dir {
                JetDir::Left => shape_pos + P(-1, 0),
                JetDir::Right => shape_pos + P(1, 0),
            };
            if try_pos.0 >= 0
                && (try_pos.0 + shape.width() as i64 - 1) < 7  // -1 for reasons
                && !self.intersects(&shape, &try_pos)
            {
                shape_pos = try_pos;
            }
            let try_down = P(shape_pos.0, shape_pos.1 - 1);
            if self.intersects(&shape, &try_down) {
                break;
            } else {
                shape_pos = try_down;
            }
        }
        self.0.extend(shape.points_at_pos(&shape_pos).iter());
        dir_index
    }

    fn intersects(&self, shape: &Shape, pos: &P) -> bool {
        shape.points_at_pos(pos).iter().any(|p| self.0.contains(p))
    }

    #[allow(dead_code)]
    fn draw(&self) {
        for y in (0..=self.height()).rev() {
            print!("|");
            for x in 0..7 {
                print!("{}", if self.0.contains(&P(x, y as i64)) { "#" } else { " " });
            }
            print!("|");
            println!();
        }
    }
}

fn main() {
    let data = aoc_client::input(2022, 17).unwrap();
    // let data = ">>><<><>><<<>><>>><<<>>><<<><<<>><>><<>>".to_string();
    let jets = data
        .trim()
        .chars()
        .map(|c| match c {
            '<' => JetDir::Left,
            '>' => JetDir::Right,
            c => panic!("Bad jet char: '{c}'"),
        })
        .collect::<Vec<_>>();

    let mut jetstream = jets.iter().copied().enumerate().cycle();
    let mut map = Map::new();
    for (_, shape) in (0..2022).zip(Shape::all().into_iter().cycle()) {
        map.spawn_shape(shape, &mut jetstream);
    }
    println!("{}", map.height());

    let num_iterations: usize = 1_000_000_000_000;
    let mut map = Map::new();
    let mut signatures = HashMap::<(usize, usize), (usize, usize)>::new();
    let mut jetstream = jets.iter().copied().enumerate().cycle();
    let mut shapes = Shape::all().into_iter().enumerate().cycle().enumerate();

    let mut cycle_start = 0;
    let mut cycle_start_height = 0;
    let mut cycle_len = 0;
    let mut cycle_height = 0;
    // Let settle into cycle
    println!("Warm up");
    for (_, (_, (_, shape))) in (0..10000).zip(&mut shapes) {
        map.spawn_shape(shape, &mut jetstream);
    }
    println!("Okay let's go");
    while let Some((i, (shape_i, shape))) = shapes.next() {
        let dir_i = map.spawn_shape(shape, &mut jetstream);
        let sig = (shape_i, dir_i);
        if !signatures.contains_key(&sig) {
            signatures.insert(sig, (i, map.height())); // TODO: map.height() is slow
        } else {
            let (prev_i, prev_height) = signatures[&sig];
            cycle_start = prev_i;
            cycle_start_height = prev_height;
            cycle_len = i - prev_i;
            cycle_height = map.height() - prev_height;
            break;
        }
    }
    println!("Found cycle! len {cycle_len} height {cycle_height}");
    {
        let start_height = map.height();
        for (_, (_, (_, shape))) in (0..cycle_len).zip(&mut shapes) {
            map.spawn_shape(shape, &mut jetstream);
        }
        println!("Validate cycle height 1: {} ?= {cycle_height}", map.height() - start_height);
    }
    {
        let start_height = map.height();
        for (_, (_, (_, shape))) in (0..cycle_len).zip(&mut shapes) {
            map.spawn_shape(shape, &mut jetstream);
        }
        println!("Validate cycle height 2: {} ?= {cycle_height}", map.height() - start_height);
    }

    let num_cycles = (num_iterations - cycle_start) / cycle_len;
    let additional_shapes = (num_iterations - cycle_start) % cycle_len;
    let prev_height = map.height();
    for (_, (_, (_, shape))) in (0..additional_shapes).zip(shapes) {
        map.spawn_shape(shape, &mut jetstream);
    }
    let additional_height = map.height() - prev_height;
    // For some reason I'm _really_ not clear on, this returns a value that's 1 larger
    // than the one AOC reports.
    println!("{}", cycle_start_height + num_cycles * cycle_height + additional_height);
}
