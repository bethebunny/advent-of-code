use std::{num::ParseIntError, rc::Rc};

use lazy_static::lazy_static;
use regex::Regex;

mod aoc_client;

#[derive(Debug)]
enum Error {
    ParseMonkeyError(String),
    ParseIntError(ParseIntError),
}

type Item = u128;

#[derive(Debug, Copy, Clone)]
enum WorryLevel {
    Low = 3,
    High = 1,
}

impl From<ParseIntError> for Error {
    fn from(error: ParseIntError) -> Self {
        Error::ParseIntError(error)
    }
}

lazy_static! {
    static ref MONKEY_REGEX: Regex = Regex::new(
        r"Monkey (?P<monkey_id>\d+):
  Starting items: (?P<items>[\d, ]+)
  Operation: new = old (?P<op_type>[+*]) (?P<operand>(?:\d+|old))
  Test: divisible by (?P<test_divisible_by>\d+)
    If true: throw to monkey (?P<pass_test_monkey>\d+)
    If false: throw to monkey (?P<fail_test_monkey>\d+)"
    )
    .unwrap();
}

#[derive(Clone)]
struct Monkey {
    id: usize,
    items: Vec<Item>,
    op: Rc<dyn Fn(Item) -> Item>,
    test: Rc<dyn Fn(Item) -> bool>,
    divisor: Item,
    pass_test_monkey: usize,
    fail_test_monkey: usize,
}

impl Monkey {
    fn parse(s: &str) -> Result<Monkey, Error> {
        let caps = MONKEY_REGEX
            .captures(s)
            .ok_or(Error::ParseMonkeyError(s.to_string()))?;
        Ok(Monkey {
            id: caps["monkey_id"].parse()?,
            items: caps["items"]
                .split(", ")
                .map(|i| i.parse())
                .collect::<Result<Vec<_>, _>>()?,
            op: {
                // supports usize or "old" operand
                let operand: Option<Item> = caps["operand"].parse().ok();
                assert!(operand.is_some() || &caps["operand"] == "old");

                match &caps["op_type"] {
                    "*" => Rc::new(move |v| v * operand.unwrap_or(v)),
                    "+" => Rc::new(move |v| v + operand.unwrap_or(v)),
                    _ => Err(Error::ParseMonkeyError(format!(
                        "{} {operand:?}",
                        &caps["op_type"]
                    )))?,
                }
            },
            test: {
                let divisible_by: Item = caps["test_divisible_by"].parse()?;
                Rc::new(move |v| v % divisible_by == 0)
            },
            divisor: caps["test_divisible_by"].parse()?,
            pass_test_monkey: caps["pass_test_monkey"].parse()?,
            fail_test_monkey: caps["fail_test_monkey"].parse()?,
        })
    }

    fn inspect_items(&mut self, worry_level: WorryLevel) -> Vec<(usize, Item)> {
        self.items
            .drain(..)
            .map(|item| {
                let item = (self.op)(item) / (worry_level as Item);
                let next_monkey = match (self.test)(item) {
                    true => self.pass_test_monkey,
                    false => self.fail_test_monkey,
                };
                (next_monkey, item)
            })
            .collect()
    }
}

fn full_round(monkies: &mut Vec<Monkey>, worry_level: WorryLevel) -> Vec<usize> {
    // Keep item levels "under control"
    let modulus: Item = monkies.iter().map(|m| m.divisor).product();
    for monkey in monkies.iter_mut() {
        monkey.items = monkey.items.iter().map(|&i| i % modulus).collect();
    }

    let mut monkey_inspections: Vec<usize> = vec![];
    for i in 0..monkies.len() {
        let items_thrown = monkies[i].inspect_items(worry_level);
        monkey_inspections.push(items_thrown.len());
        for (to_monkey, item) in items_thrown {
            monkies[to_monkey].items.push(item);
        }
    }
    monkey_inspections
}

fn monkey_business(monkies: &Vec<Monkey>, worry_level: WorryLevel, num_rounds: usize) -> usize {
    let mut monkies = monkies.clone();
    let mut monkey_inspections = vec![0; monkies.len()];
    for _round in 0..num_rounds {
        println!("{_round}");
        let round_inspections = full_round(&mut monkies, worry_level);
        for (i, inspections) in round_inspections.iter().enumerate() {
            monkey_inspections[i] += inspections;
        }
    }
    monkey_inspections.sort();
    monkey_inspections.iter().rev().take(2).product()
}

fn main() {
    let data = aoc_client::input(2022, 11).unwrap();
    let monkies = data
        .split("\n\n")
        .map(Monkey::parse)
        .collect::<Result<Vec<_>, _>>()
        .unwrap();
    for (i, monkey) in monkies.iter().enumerate() {
        assert!(i == monkey.id);
    }

    println!("{}", monkey_business(&monkies, WorryLevel::Low, 20));
    println!("{}", monkey_business(&monkies, WorryLevel::High, 10000));
}
