# sheepyRTP
this is a pretty straightforward rtp plugin. it runs async, so ideally won't crash your server. all it implements is a single command, ``/RTP [radius]``. the default spawn radius is 10,000 x 10,000 blocks.
in addition, this plugin will ensure at least 15 wilderness chunks exist between the rtp location and any towny claims, check for water, lava, and cacti, as well as make sure you do not spawn inside any structures.  

this plugin was primarily designed for use with townyAI, and so I have not (yet) put in the effort to load parameters from a config. as such, you would need to recompile sheepyRTP to change the default spawn radius, change the wilderness gap, or make any other setting change.
fortunately, this is trivial provided you have a java development environment setup. If other people want to use this plugin for some reason, I'm not opposed to adding a config in the future. 

## usage with bots
this plugin was primarily designed for use with townyAI, which uses mineflayer bots. If you want to use this RTP plugin with your bots, it provides a useful optional parameter [ack]. an example of this would be
```
/rtp 5000 2e84be58534a466a9d1055f7b7c3773c
```
the plugin will whipser ``2e84be58534a466a9d1055f7b7c3773c`` to the bot which ran the command. it's pretty easy to then await this response before proceeding with the rest of its code.

a full example of this would be:
```
bot.chat(`/rtp 5000 ${ack_code}`);

  // Wait for a whisper from 'Server' containing the ack_code
  await new Promise((resolve) => {
    function onWhisper(from, msg) {
      if (from === 'Server' && msg.includes(ack_code)) {
        bot.removeListener('whisper', onWhisper);
        resolve();
      }
    }
    bot.on('whisper', onWhisper);
  });

  bot.chat(`RTP confirmed with ack_code: ${ack_code}`);
```
