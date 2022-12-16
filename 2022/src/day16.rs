#![feature(iterator_try_collect)]

use std::collections::{HashMap, HashSet};
use std::hash::{Hash, Hasher};
use std::num::ParseIntError;
use std::rc::Rc;

use core::fmt::Debug;

use lazy_static::lazy_static;
use regex::Regex;

mod aoc_client;

lazy_static! {
    static ref VALVE_RE: Regex = Regex::new(
        r"Valve (?P<valve>.*) has flow rate=(?P<rate>\d+); tunnels? leads? to valves? (?P<edges>.*)$"
    )
    .unwrap();
}

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

#[derive(PartialEq, Eq, Clone, Debug)]
struct Valve {
    id: String,
    flow_rate: usize,
    edges: HashMap<String, usize>,
}

impl Valve {
    fn parse(s: &str) -> Result<Valve, Error> {
        let caps = VALVE_RE.captures(s).ok_or(Error::ParseError(s.to_string()))?;
        Ok(Valve {
            id: caps["valve"].to_string(),
            flow_rate: caps["rate"].parse()?,
            edges: caps["edges"].split(", ").map(|s| (s.to_string(), 1)).collect(),
        })
    }

    // Include all paths to other nodes with flow rates
    fn compress(&self, valves: &HashMap<String, &Valve>) -> Valve {
        let mut edges = self.edges.clone();
        let mut queue: Vec<(String, usize)> = edges.iter().map(|(s, &d)| (s.clone(), d)).collect();
        while let Some((edge, distance)) = queue.pop() {
            if edge == self.id || edges.contains_key(&edge) && edges[&edge] < distance {
                continue;
            }
            edges.insert(edge.clone(), distance);

            let neighbor = valves[&edge];
            for (next_edge, nd) in neighbor.edges.iter() {
                queue.push((next_edge.clone(), distance + nd));
            }
        }
        let edge_names: Vec<String> = edges.keys().cloned().collect();
        for edge in edge_names {
            if valves[&edge].flow_rate == 0 {
                edges.remove(&edge);
            }
        }
        Valve { id: self.id.clone(), flow_rate: self.flow_rate, edges }
    }
}

#[derive(PartialEq, Eq, Clone, Debug)]
struct State {
    // I have _no &^!@*&$ idea_ how to implement memoization using &str.
    // The reference lifetime checker accepted zero of my blood sacrifices.
    // CLONE ALL THE STRINGS!
    time: usize,
    loc: String,
    open_valves: Rc<HashSet<String>>,
}

impl Hash for State {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.time.hash(state);
        self.loc.hash(state);
        self.open_valves.iter().for_each(|v| v.hash(state));
    }
}

impl State {
    fn choices(&self, valves: &HashMap<String, &Valve>) -> Vec<(State, usize)> {
        Vec::from_iter(
            valves[&self.loc]
                .edges
                .iter()
                .filter(|(_, &time)| time + 1 < self.time)
                .filter(|(next_loc, _)| !self.open_valves.contains(*next_loc))
                .map(|(next_loc, &time)| {
                    let open_valves =
                        self.open_valves.iter().chain(std::iter::once(next_loc)).cloned();
                    let next_state = State {
                        time: self.time - time - 1,
                        loc: next_loc.clone(),
                        open_valves: Rc::new(open_valves.collect()),
                    };
                    let ltv = valves[next_loc].flow_rate * (self.time - time - 1);
                    (next_state, ltv)
                }),
        )
    }
}

fn plan_path(valves: &HashMap<String, &Valve>, state: &State) -> usize {
    let choices = state.choices(valves);
    choices.iter().map(|(choice, value)| value + plan_path(valves, choice)).max().unwrap_or(0)
}

#[derive(PartialEq, Eq, Clone, Debug)]
struct ElephantState {
    me_time: usize,
    me: String,
    elephant_time: usize,
    elephant: String,
    open_valves: Rc<HashSet<String>>,
}

impl ElephantState {
    fn choices(&self, valves: &HashMap<String, &Valve>) -> Vec<(ElephantState, usize)> {
        if self.me_time > self.elephant_time {
            // My turn!
            Vec::from_iter(
                valves[&self.me]
                    .edges
                    .iter()
                    .filter(|(_, &time)| time + 1 < self.me_time)
                    .filter(|(next_loc, _)| !self.open_valves.contains(*next_loc))
                    .map(|(next_loc, &time)| {
                        let open_valves =
                            self.open_valves.iter().chain(std::iter::once(next_loc)).cloned();
                        let next_state = ElephantState {
                            me_time: self.me_time - time - 1,
                            me: next_loc.clone(),
                            elephant_time: self.elephant_time,
                            elephant: self.elephant.clone(),
                            open_valves: Rc::new(open_valves.collect()),
                        };
                        let ltv = valves[next_loc].flow_rate * (self.me_time - time - 1);
                        (next_state, ltv)
                    }),
            )
        } else {
            // Elephant goes
            Vec::from_iter(
                valves[&self.elephant]
                    .edges
                    .iter()
                    .filter(|(_, &time)| time + 1 < self.elephant_time)
                    .filter(|(next_loc, _)| !self.open_valves.contains(*next_loc))
                    .map(|(next_loc, &time)| {
                        let open_valves =
                            self.open_valves.iter().chain(std::iter::once(next_loc)).cloned();
                        let next_state = ElephantState {
                            me_time: self.me_time,
                            me: self.me.clone(),
                            elephant_time: self.elephant_time - time - 1,
                            elephant: next_loc.clone(),
                            open_valves: Rc::new(open_valves.collect()),
                        };
                        let ltv = valves[next_loc].flow_rate * (self.elephant_time - time - 1);
                        (next_state, ltv)
                    }),
            )
        }
    }
}

fn plan_path_with_elephant(valves: &HashMap<String, &Valve>, state: &ElephantState) -> usize {
    let choices = state.choices(valves);
    choices
        .iter()
        .map(|(choice, value)| value + plan_path_with_elephant(valves, choice))
        .max()
        .unwrap_or(0)
}

fn main() {
    let data = aoc_client::input(2022, 16).unwrap();
    let valves: Vec<Valve> = data.lines().map(Valve::parse).try_collect().unwrap();
    let valve_map: HashMap<String, &Valve> = valves.iter().map(|v| (v.id.clone(), v)).collect();

    let compressed_valves: Vec<Valve> = valves
        .iter()
        .map(|v| v.compress(&valve_map))
        .filter(|v| v.id == "AA" || v.flow_rate > 0)
        .collect();
    let compressed_valve_map: HashMap<String, &Valve> =
        compressed_valves.iter().map(|v| (v.id.clone(), v)).collect();

    // valve_map.values().for_each(|v| println!("{v:?}"));
    // println!("\n\nCOMPRESSED:");
    // compressed_valve_map.values().for_each(|v| println!("{v:?}"));

    let start_state =
        State { time: 30, loc: String::from("AA"), open_valves: Rc::new(HashSet::new()) };
    let most_pressure_released = plan_path(&compressed_valve_map, &start_state);
    println!("{most_pressure_released}");

    let start_state = ElephantState {
        me_time: 26,
        me: String::from("AA"),
        elephant_time: 26,
        elephant: String::from("AA"),
        open_valves: Rc::new(HashSet::from_iter([String::from("AA")])),
    };
    let most_pressure_released = plan_path_with_elephant(&compressed_valve_map, &start_state);
    println!("{most_pressure_released}");
}
