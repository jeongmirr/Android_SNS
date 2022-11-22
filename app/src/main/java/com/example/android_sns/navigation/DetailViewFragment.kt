package com.example.android_sns.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.android_sns.R
import com.example.android_sns.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {

    var firestore: FirebaseFirestore? = null
    var uid : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()

        init {
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()

                    //Some times, This code return null of querySnapshot when it signout
                    if(querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUidList.add(snapshot.id)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as CustomViewHolder).itemView

            // 유저 ID
            viewholder.detailviewitem_profile_textview.text = contentDTOs!![position].userId

            // 이미지
           Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.detailviewitem_imageview_content)

            // 게시물 설명 텍스트
            viewholder.detailviewitem_explain_textview.text = contentDTOs!![position].explain

            // 좋아요 카운트
            viewholder.detailviewitem_favoritecounter_textview.text = "Likes " + contentDTOs!![position].favoriteCount

            // 프로필 이미지
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.detailviewitem_profile_image)

            // 좋아요 버튼 클릭시
            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }

            // 좋아요 버튼 표시
            if(contentDTOs!![position].favorites.containsKey(uid)){
                // 좋아요 버튼 클릭한 상태
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{
                // 좋아요 버튼 클릭 안한 상태
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            // 프로필 이미지 클릭 시 상대방 유저 정보로 이동
            viewholder.detailviewitem_profile_image.setOnClickListener{
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }
            viewholder.detailviewitem_comment_imageview.setOnClickListener { v ->
                var intent = Intent(v.context,CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[position])
                startActivity(intent)
            }
        }

        fun favoriteEvent(position: Int) {
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->

                var uid = FirebaseAuth.getInstance().currentUser?.uid
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)) {
                    // 좋아요 버튼 클릭되어 있을 경우
                    contentDTO.favoriteCount = contentDTO.favoriteCount - 1
                    contentDTO.favorites.remove(uid)
                } else {
                    // 좋아요 버튼 클릭되어 있지 않을 경우
                    contentDTO.favoriteCount = contentDTO.favoriteCount + 1
                    contentDTO.favorites?.set(uid!!, true)
                }
                transaction.set(tsDoc,contentDTO)
            }
        }

    }
}