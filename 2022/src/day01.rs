mod aoc_client;

use std::collections::HashSet;

fn main() {
    let data = aoc_client::input(2018, 1).unwrap();
    let lines = data.trim().split("\n");
    let deltas = lines
        .map(|s| s.parse::<i32>())
        .collect::<Result<Vec<i32>, _>>()
        .unwrap();
    println!("{}", deltas.iter().sum::<i32>());

    let freqs = deltas.into_iter()
        .cycle()
        // Scan docs are bullshit.
        // - Iterating over a scan unwraps the returned Option
        // - None as a return indicates stop iteration
        .scan(0, |freq, delta| {
            *freq += delta;
            Some(*freq)
        });
    let mut seen_freqs = freqs.scan(
        HashSet::new(),
        |seen, freq| {
            let res = seen.contains(&freq);
            seen.insert(freq);
            if res { Some(Some(freq)) } else { Some(None) }
        }
    // Option implements IntoIter, but you can't use
    // .flatten on an Iterable<Option<T>> like you'd expect.
    ).filter_map(std::convert::identity);
    println!("{}", seen_freqs.next().unwrap());
    //println!("{}", &freqs.next().unwrap());
}
