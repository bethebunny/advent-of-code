#![feature(iterator_try_collect)]

use std::collections::HashMap;
use std::num::ParseIntError;

use lazy_static::lazy_static;
use regex::Regex;

mod aoc_client;

lazy_static! {
    static ref OP_RE: Regex =
        Regex::new(r"^(?P<name>.*): (?P<left>.*) (?P<op>[+\-*/]) (?P<right>.*)$").unwrap();
    static ref NUM_RE: Regex = Regex::new(r"^(?P<name>.*): (?P<value>-?\d+)$").unwrap();
}

#[derive(Debug)]
enum Error {
    ParseIntError(ParseIntError),
    InvalidOp(String),
    ParseError(String),
}

impl From<ParseIntError> for Error {
    fn from(e: ParseIntError) -> Error {
        Error::ParseIntError(e)
    }
}

#[derive(Debug, Clone, Copy)]
enum Op {
    Add,
    Subtract,
    Multiply,
    Divide,
}

impl Op {
    fn parse(s: &str) -> Result<Op, Error> {
        Ok(match s {
            "+" => Op::Add,
            "-" => Op::Subtract,
            "*" => Op::Multiply,
            "/" => Op::Divide,
            _ => Err(Error::InvalidOp(s.to_string()))?,
        })
    }
}

#[derive(Debug, Clone)]
enum Job {
    Num(f64),
    Op { op: Op, left: String, right: String },
}

struct Jobs {
    jobs: HashMap<String, Job>,
    value_fn: Box<dyn Fn(&Jobs, &String) -> f64>,
}

impl Jobs {
    fn value(&self, name: &String) -> f64 {
        let job = &self.jobs[name];
        match job {
            Job::Num(value) => *value,
            Job::Op { op, left, right } => {
                let left = (self.value_fn)(self, &left);
                let right = (self.value_fn)(self, &right);
                match op {
                    Op::Add => left + right,
                    Op::Subtract => left - right,
                    Op::Multiply => left * right,
                    Op::Divide => left / right,
                }
            }
        }
    }
}

impl Job {
    fn parse(s: &str) -> Result<(String, Job), Error> {
        if let Some(caps) = OP_RE.captures(s) {
            Ok((
                caps["name"].to_string(),
                Job::Op {
                    op: Op::parse(&caps["op"])?,
                    left: caps["left"].to_string(),
                    right: caps["right"].to_string(),
                },
            ))
        } else if let Some(caps) = NUM_RE.captures(s) {
            Ok((caps["name"].to_string(), Job::Num(caps["value"].parse::<i64>()? as f64)))
        } else {
            Err(Error::ParseError(s.to_string()))
        }
    }
}

static EPSILON: f64 = 1e-2;

fn estimate_derivative(f: &dyn Fn(f64) -> f64, x: f64) -> f64 {
    (f(x + EPSILON) - f(x)) / EPSILON
}

fn newtons_method(f: &dyn Fn(f64) -> f64) -> f64 {
    let mut x = 0f64;
    loop {
        let y = f(x);
        println!("f({x}) = {y}");
        if y.abs() < EPSILON {
            return x;
        }
        let dydx = estimate_derivative(f, x);
        println!("f'({x}) = {dydx}");
        x -= y / dydx;
    }
}

fn main() {
    let data = aoc_client::input(2022, 21).unwrap();
    let jobs: HashMap<String, Job> = data.lines().map(Job::parse).try_collect().unwrap();
    let jobs = Jobs { jobs, value_fn: Box::new(Jobs::value) };
    println!("{}", jobs.value(&"root".to_string()));

    let humn = newtons_method(&|x| {
        // My plan was to avoid a clone with this closure nonsense, but that failed,
        // so I'm just rolling with it for now :P
        let mut jobs = Jobs { jobs: jobs.jobs.clone(), value_fn: Box::new(Jobs::value) };
        jobs.value_fn = Box::new(move |jobs, name| match name.as_str() {
            "humn" => x,
            _ => jobs.value(name),
        });
        if let Job::Op { left, right, .. } = &jobs.jobs["root"] {
            jobs.value(left) - jobs.value(right)
        } else {
            panic!()
        }
    });
    println!("{humn}");
}
