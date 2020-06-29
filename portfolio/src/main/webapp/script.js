// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

window.onload = function() {
  const pictureAndCaptionButton =
      document.getElementById('picture-and-caption-button');
  pictureAndCaptionButton.addEventListener('click', showPictureAndCaption);
  getComments();
  const deleteCommentsButton =
      document.getElementById('delete-comments-button');
  deleteCommentsButton.addEventListener('click', deleteComments);
};

const SNOW_IMG_CAPTION = 'This is from NSBE Nationals last year when I saw ' +
    'snow for the first time. I later threw on some more jackets, sweats, ' +
    'gloves, and socks and played with the snow for a couple of hours.';

const CHICAGO_IMG_CAPTION = 'Chicago is extremely cold when the weather is ' +
    'windy. I was very happy to find out the train stations had heaters.';

const BABY_IMG_CAPTION = 'This baby picture of me is the only time I know ' +
    'of that I had longer hair than I do now.';

const MILKSHAKE_IMG_CAPTION = 'Milkshakes are my favorite food/drink. I had ' +
    'to have a picture with one when I got to school.';

const FISH_IMG_CAPTION = 'I took AP art my senior year of high school. ' +
    'I\'ve been around photography my whole life so it\'s what I chose ' +
    'to do for my portfolio. ';

function showPictureAndCaption() {
  const picturesAndCaptions = [
    ['images/Snow.jpg', SNOW_IMG_CAPTION],
    ['images/Chicago.jpg', CHICAGO_IMG_CAPTION],
    ['images/Baby.jpg', BABY_IMG_CAPTION],
    ['images/Milkshake.JPG', MILKSHAKE_IMG_CAPTION],
    ['images/Fish.PNG', FISH_IMG_CAPTION],
  ];

  const imgIndex = Math.floor(Math.random() * 5);

  const imgElement = document.createElement('img');
  imgElement.src = picturesAndCaptions[imgIndex][0];
  const imgCaption = picturesAndCaptions[imgIndex][1];

  const pictureContainer = document.getElementById('picture');
  pictureContainer.innerHTML = '';
  pictureContainer.appendChild(imgElement);

  const captionContainer = document.getElementById('caption');
  captionContainer.innerHTML = '';
  captionContainer.innerHTML = imgCaption;
}

async function getComments() {
  const commentAmount = document.getElementById('comment-amount').value;
  const response = await fetch('/data?max-comments=' + commentAmount);
  const comments = await response.json();
  const listElement = document.getElementById('comments');
  listElement.innerHTML = '';
  comments.forEach((comment) => {
    listElement.appendChild(createListElement(comment));
  });
}

function createListElement(text) {
  const liElement = document.createElement('li');
  liElement.innerText = text;
  return liElement;
}

async function deleteComments() {
  await fetch('/delete-data', {method: 'post'});
  getComments();
}
