#![feature(let_chains)]
#![feature(iter_advance_by)]
#![feature(iter_next_chunk)]

mod aoc_client;

use lazy_static::lazy_static;
use regex::Regex;

#[derive(Debug, Clone)]
struct Crates(Vec<Vec<char>>);

#[derive(Debug)]
struct Move {
    num: usize,
    from: usize,
    to: usize,
}

impl Crates {
    fn parse(crates_raw: &str) -> Option<Crates> {
        // Hmm there's probably a better way to do this, especially since I don't use `names`
        let mut raw_stacks = crates_raw.lines().rev();
        let names = raw_stacks.next()?;
        let num_stacks = names.len() / 4 + 1;
        // Stacks are dense at the bottom, potentially sparser near the top
        // All stack values are 1 character, eg. `[X]`
        let mut stacks: Vec<Vec<char>> = vec![vec![]; num_stacks];
        for stack in raw_stacks {
            for stack_idx in 0..num_stacks {
                if let Some(chr) = stack.chars().nth(1 + 4 * stack_idx) && chr != ' ' {
                    stacks[stack_idx].push(chr);
                }
            }
        }
        Some(Crates(stacks))
    }

    fn apply_move(&mut self, move_: &Move) {
        // from and to use 1-based indexing
        for _ in 0..move_.num {
            let popped = self.0[move_.from - 1].pop().unwrap();
            self.0[move_.to - 1].push(popped);
        }
    }

    fn apply_move_retain_order(&mut self, move_: &Move) {
        // from and to use 1-based indexing
        let from_stack = &mut self.0[move_.from - 1];
        let mut popped = from_stack.split_off(from_stack.len() - move_.num);
        self.0[move_.to - 1].append(&mut popped);
    }

    fn tops(&self) -> impl Iterator<Item = &char> {
        self.0.iter().flat_map(|v| v.last())
    }
}

lazy_static! {
    static ref MOVE_REGEX: Regex = Regex::new(r"move (\d+) from (\d+) to (\d+)").unwrap();
}

impl Move {
    fn parse(raw: &str) -> Option<Move> {
        let groups = MOVE_REGEX.captures(raw)?;
        Some(Move {
            num: groups.get(1)?.as_str().parse::<usize>().ok()?,
            from: groups.get(2)?.as_str().parse::<usize>().ok()?,
            to: groups.get(3)?.as_str().parse::<usize>().ok()?,
        })
    }
}

fn main() {
    let data = aoc_client::input(2022, 5).unwrap();
    let [crates_raw, moves_raw] = data.split("\n\n").next_chunk::<2>().unwrap();
    let mut crates = Crates::parse(crates_raw).unwrap();
    let mut crates2 = crates.clone();
    let moves = moves_raw.lines().flat_map(Move::parse);

    for move_ in moves {
        crates.apply_move(&move_);
        crates2.apply_move_retain_order(&move_);
    }

    println!("{}", crates.tops().collect::<String>());
    println!("{}", crates2.tops().collect::<String>());
}
