// use std::collections::HashMap;
use std::env;
use std::sync::Arc;
use std::{fs, fs::File};
use std::{io, io::Write};

use reqwest::{blocking::Client, cookie::Jar, Url};

static CACHE_DIR: &'static str = ".data";

#[derive(Debug)]
pub enum Error {
    Fetch(reqwest::Error),
    Cache(io::Error),
}

impl From<io::Error> for Error {
    fn from(error: io::Error) -> Self {
        Error::Cache(error)
    }
}

impl From<reqwest::Error> for Error {
    fn from(error: reqwest::Error) -> Self {
        Error::Fetch(error)
    }
}

fn session_id() -> String {
    env::var("AOC_SESSION_ID").expect("Must specify AOC_SESSION_ID in env")
}

fn input_url(year: u16, day: u8) -> String {
    format!("https://adventofcode.com/{}/day/{}/input", year, day)
}

fn cache_path(year: u16, day: u8) -> String {
    format!("{}/{}.{}.txt", CACHE_DIR, year, day)
}

fn validate_or_create_cache_directory() -> Result<(), io::Error> {
    let meta = fs::metadata(CACHE_DIR);
    if !meta.is_ok() {
        fs::create_dir_all(CACHE_DIR)?;
    }
    Ok(())
}

pub fn input(year: u16, day: u8) -> Result<String, Error> {
    validate_or_create_cache_directory()?;
    let path = cache_path(year, day);
    match fs::read_to_string(&path) {
        Ok(data) => Ok(data),
        Err(_) => {
            let data = load_input(year, day)?;
            let mut file = File::create(path)?;
            file.write_all(data.as_bytes())?;
            Ok(data)
        }
    }
}

pub fn load_input(year: u16, day: u8) -> Result<String, reqwest::Error> {
    println!("Loading data from website!");
    let jar = Arc::new(Jar::default());
    let url: Url = "https://adventofcode.com".parse().unwrap();
    jar.add_cookie_str(&format!("session={}", session_id()), &url);
    let client = Client::builder().cookie_provider(jar).build()?;
    let request = client.get(input_url(year, day)).build()?;
    let response = client.execute(request)?;
    Ok(response.text()?)
}
