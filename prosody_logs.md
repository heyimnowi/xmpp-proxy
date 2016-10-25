## Steps

- chmod 400 my-key-pair.pem
- ssh -i ~/.ssh/my-key-pair.pem ubuntu@ec2-54-69-136-236.us-west-2.compute.amazonaws.com
- tail -f /var/log/prosody/prosody.*