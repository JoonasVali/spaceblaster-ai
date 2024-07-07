# SpaceBlaster AI Narrator

This is an AI narrator for the SpaceBlaster game. It uses a large language model to generate text based on the game state. 
The game state is sent to the AI narrator as a text prompt, and the AI narrator generates a response based on the prompt.
Then the text received from the language model and sent to text-to-speech model for generating audio. 

The final audio track is then sewn together from the audio clips generated by the text-to-speech model. The output
of this process is a full length audio file of the event file, that was provided as input to the AI narrator.

At the moment it remains for the user to manually combine the audio track with the video track of the game.

Note that it's not a real time AI narrator, but rather a post-processing tool for generating interesting commentary for 
the game, as a proof of concept and interesting use case for AI tools.

### Implementation and external services used

The AI narrator is implemented in Java and uses the OpenAI API for the language model and the ElevenLabs API for the 
text-to-speech model.

## SpaceBlaster Game

The game is available at:
https://github.com/JoonasVali/SpaceBlaster

## Requirements

* Java 21+
* Maven 3.3+
* OpenAI token (paid subscription)
* ElevenLabs API key (some free usage possible, but generally paid subscription is required)

## Getting started

1) Save OpenAI token as `OPENAI_TOKEN` environment variable.
2) Save ElevenLabs api key as `ELEVENLABS_API_KEY` environment variable.
3) Clone the [SpaceBlaster repository](https://github.com/JoonasVali/SpaceBlaster) and `mvn clean install` it to get the spaceblaster `event` library. 
4) Clone the SpaceBlaster-ai repository 
5) In `Launch` class change the constants `EVENT_DATA_PATH` and `SOUND_OUTPUT_DIRECTORY_ROOT` to point to the generated 
event data file and to a directory which serves as a workspace for the output of this project.
6) Run the main class `Launch` to generate the audio file. It's a long process to avoid rate limiting.
Adjust or remove the sleeps in case your accounts have higher rate limits.

### Activating the event system in SpaceBlaster

Follow instructions [here](https://github.com/JoonasVali/SpaceBlaster?tab=readme-ov-file#event-system).

Note that generally the flow should be:
1) Play the game with event system activated and record the video of it.
2) Enter the event data file to the AI narrator.
3) Run the script
4) Combine the audio track with the video track.

### Example runs:

* [Longer game, Narrated by Fin 1](https://www.youtube.com/watch?v=19sxnWyOv_M) 
* [Longer game, Narrated by Fin 2](https://www.youtube.com/watch?v=aLvp6on_FHs) 
* [Short run, narrated by Liam](https://www.youtube.com/watch?v=4tj6QLNC0uU) 
* [Short run, narrated by Mimi](https://www.youtube.com/watch?v=Xq0JhjDl6ps) 

### IMPORTANT

Since this project is using external services (with paid subscriptions), it's important to note that the project author 
does not take any responsibility for the usage of the services or for the used tokens. The user is responsible for the
usage of the services and for the costs that may arise from the usage of the services. It's possible that the project
contains bugs or other issues that may cause the services to be used in an unintended way or which might cause unintended
costs. The project author does not take any responsibility for these issues or costs. Before launching, set reasonable 
limits in your accounts to avoid accidental overspending. The user activating this program will take full responsibility 
of any negative effects it might have in spending the credits in external services.
